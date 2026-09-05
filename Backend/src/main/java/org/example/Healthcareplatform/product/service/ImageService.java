package org.example.Healthcareplatform.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.dto.ImageResponse;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.entity.ProductImage;
import org.example.Healthcareplatform.product.repository.ProductImageRepository;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Transactional
    public ImageResponse uploadImage(Long productId, MultipartFile file) {
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File too large (max 5MB)");
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String ext = resolveExtension(file);
            String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String thumbUrl = generateThumbnail(target.toFile(), dir.resolve("thumb-" + storedName).toFile());
            long count = imageRepository.countByProductId(productId);
            ProductImage image = ProductImage.builder()
                    .productId(productId)
                    .fileName(storedName)
                    .contentType(file.getContentType())
                    .url("/uploads/" + storedName)
                    .thumbnailUrl(thumbUrl)
                    .primary(count == 0)
                    .sortOrder((int) count)
                    .build();
            ProductImage saved = imageRepository.save(image);
            if (count == 0) {
                productRepository.findById(productId).ifPresent(p -> {
                    p.setImageUrl(saved.getUrl());
                    productRepository.save(p);
                });
            }
            log.info("Uploaded image id={} for product={}", saved.getId(), productId);
            return ImageResponse.fromEntity(saved);
        } catch (IOException e) {
            log.error("Failed to store image for product={}", productId, e);
            throw new IllegalStateException("Failed to store image file", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(Long productId) {
        return imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .map(ImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ImageResponse setPrimary(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
        imageRepository.clearPrimary(image.getProductId());
        image.setPrimary(true);
        ProductImage saved = imageRepository.save(image);
        productRepository.findById(saved.getProductId()).ifPresent(p -> {
            p.setImageUrl(saved.getUrl());
            productRepository.save(p);
        });
        return ImageResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
        boolean wasPrimary = image.getPrimary();
        imageRepository.delete(image);
        deleteFile(image.getFileName());
        if (image.getThumbnailUrl() != null) deleteFile(image.getThumbnailUrl().replace("/uploads/", ""));
        if (wasPrimary) {
            imageRepository.findByProductIdOrderBySortOrderAscIdAsc(image.getProductId()).stream()
                    .findFirst().ifPresent(next -> {
                        next.setPrimary(true);
                        imageRepository.save(next);
                        productRepository.findById(next.getProductId()).ifPresent(p -> {
                            p.setImageUrl(next.getUrl());
                            productRepository.save(p);
                        });
                    });
        }
        log.info("Deleted image id={}", imageId);
    }

    private String generateThumbnail(File source, File thumbFile) {
        try {
            BufferedImage original = ImageIO.read(source);
            if (original == null) return null;
            int targetWidth = 240;
            double ratio = (double) original.getHeight() / original.getWidth();
            int targetHeight = (int) (targetWidth * ratio);
            BufferedImage thumb = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();
            ImageIO.write(thumb, "jpg", thumbFile);
            return "/uploads/" + thumbFile.getName();
        } catch (IOException e) {
            log.warn("Thumbnail generation failed for {}", source.getName(), e);
            return null;
        }
    }

    private String resolveExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        String ext = "";
        if (name != null && name.contains(".")) {
            ext = name.substring(name.lastIndexOf('.')).toLowerCase();
        }
        return ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)") ? ext : ".jpg";
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(fileName));
        } catch (IOException e) {
            log.warn("Could not delete file {}", fileName, e);
        }
    }
}
