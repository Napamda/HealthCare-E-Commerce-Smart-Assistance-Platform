package org.example.Healthcareplatform.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.inventory.dto.StockInfoResponse;
import org.example.Healthcareplatform.inventory.entity.StockHistory;
import org.example.Healthcareplatform.inventory.entity.StockReservation;
import org.example.Healthcareplatform.inventory.repository.StockHistoryRepository;
import org.example.Healthcareplatform.inventory.repository.StockReservationRepository;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockReservationRepository reservationRepository;

    public record ReserveItem(Long productId, Integer quantity) {}

    // ============ Stock read ============

    @Transactional(readOnly = true)
    public List<StockInfoResponse> getAllStockInfo() {
        List<StockInfoResponse> result = new java.util.ArrayList<>();
        for (Product p : productRepository.findAll()) {
            try {
                result.add(toStockInfo(p, false));
            } catch (Exception e) {
                log.warn("Skipping stock row for product {}: {}", p.getId(), e.getMessage());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public StockInfoResponse getStockInfo(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return toStockInfo(product, true);
    }

    @Transactional(readOnly = true)
    public long getAvailableQuantity(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        int stockQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        long reserved = 0L;
        try {
            Long reservedRaw = reservationRepository.sumActiveReservedQuantity(product.getId());
            reserved = reservedRaw != null ? reservedRaw : 0L;
        } catch (Exception e) {
            log.warn("Reservation lookup failed for product {}: {}", product.getId(), e.getMessage());
        }
        return Math.max(0L, stockQty - reserved);
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> {
                    int qty = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
                    int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 5;
                    return qty <= threshold;
                })
                .map(this::toStockInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockHistoryDto> getHistory(Long productId) {
        return stockHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getReservations() {
        try {
            return reservationRepository.findAll().stream()
                    .map(this::toReservationDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load reservations: {}", e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockHistoryDto> getRecentActivity(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        try {
            return stockHistoryRepository
                    .findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, size))
                    .stream()
                    .map(this::toHistoryDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load recent stock activity: {}", e.getMessage());
            return List.of();
        }
    }

    // ============ Stock mutations ============

    @Transactional
    public Product increaseStock(Long productId, int quantity, Long userId, String note) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = getProduct(productId);
        int before = product.getStockQuantity();
        product.setStockQuantity(before + quantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.PURCHASE, quantity, before + quantity, userId, null, note);
        log.info("Increased stock for product={} by {} (new stock={})", productId, quantity, before + quantity);
        return product;
    }

    @Transactional
    public Product decreaseStock(Long productId, int quantity, Long userId, String note) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = getProduct(productId);
        if (quantity > product.getStockQuantity()) {
            throw new IllegalStateException("Insufficient stock for product " + product.getName()
                    + " (requested " + quantity + ", available " + product.getStockQuantity() + ")");
        }
        int before = product.getStockQuantity();
        product.setStockQuantity(before - quantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.ADJUSTMENT, -quantity, before - quantity, userId, null, note);
        log.info("Decreased stock for product={} by {} (new stock={})", productId, quantity, before - quantity);
        return product;
    }

    @Transactional
    public Product adjustStock(Long productId, int newQuantity, Long userId, String note) {
        if (newQuantity < 0) throw new IllegalArgumentException("Stock cannot be negative");
        Product product = getProduct(productId);
        int before = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.ADJUSTMENT, newQuantity - before, newQuantity, userId, null, note);
        log.info("Adjusted stock for product={} from {} to {} — {}", productId, before, newQuantity, note);
        return product;
    }

    @Transactional
    public Product setLowStockThreshold(Long productId, int threshold) {
        if (threshold < 0) throw new IllegalArgumentException("Threshold cannot be negative");
        Product product = getProduct(productId);
        product.setLowStockThreshold(threshold);
        productRepository.save(product);
        log.info("Updated low-stock threshold for product={} to {}", productId, threshold);
        return product;
    }

    // ============ Reservations ============

    @Transactional
    public void reserveStock(Long userId, Long orderId, List<ReserveItem> items) {
        for (ReserveItem item : items) {
            Product product = getProduct(item.productId());
            Long activeReserved = reservationRepository.sumActiveReservedQuantity(product.getId());
            long available = product.getStockQuantity() - activeReserved;
            if (available < item.quantity()) {
                releaseReservations(orderId, "Stock unavailable");
                throw new IllegalStateException("Insufficient stock for " + product.getName()
                        + " (requested " + item.quantity() + ", available " + available + ")");
            }
            StockReservation reservation = StockReservation.builder()
                    .productId(product.getId())
                    .userId(userId)
                    .quantity(item.quantity())
                    .orderId(orderId)
                    .status(StockReservation.Status.ACTIVE)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                    .build();
            reservationRepository.save(reservation);
            int stockAfter = (int) (product.getStockQuantity() - activeReserved - item.quantity());
            logHistory(product, StockHistory.ChangeType.RESERVED, -item.quantity(),
                    stockAfter, userId, orderId,
                    "Reserved for order " + orderId);
        }
        log.info("Reserved stock for order={} with {} items", orderId, items.size());
    }

    @Transactional
    public void confirmReservations(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, StockReservation.Status.ACTIVE);
        if (reservations.isEmpty()) return;
        for (StockReservation r : reservations) {
            Product product = getProduct(r.getProductId());
            if (r.getQuantity() > product.getStockQuantity()) {
                throw new IllegalStateException("Stock already sold out for product " + product.getName());
            }
            int before = product.getStockQuantity();
            product.setStockQuantity(before - r.getQuantity());
            productRepository.save(product);
            r.setStatus(StockReservation.Status.CONFIRMED);
            reservationRepository.save(r);
            logHistory(product, StockHistory.ChangeType.SALE, -r.getQuantity(),
                    before - r.getQuantity(), r.getUserId(), orderId, "Order confirmed — payment received");
        }
        log.info("Confirmed reservations for order={} — stock deducted", orderId);
    }

    @Transactional
    public void releaseReservations(Long orderId, String reason) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, StockReservation.Status.ACTIVE);
        if (reservations.isEmpty()) return;
        for (StockReservation r : reservations) {
            r.setStatus(StockReservation.Status.RELEASED);
            reservationRepository.save(r);
            productRepository.findById(r.getProductId()).ifPresent(p ->
                    logHistory(p, StockHistory.ChangeType.RESERVATION_RELEASED, r.getQuantity(),
                            p.getStockQuantity(), r.getUserId(), orderId, reason));
        }
        log.info("Released {} reservations for order={} ({})", reservations.size(), orderId, reason);
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(StockReservation.Status.ACTIVE, Instant.now());
        if (expired.isEmpty()) return;
        for (StockReservation r : expired) {
            r.setStatus(StockReservation.Status.EXPIRED);
            reservationRepository.save(r);
            productRepository.findById(r.getProductId()).ifPresent(p ->
                    logHistory(p, StockHistory.ChangeType.RESERVATION_EXPIRED, r.getQuantity(),
                            p.getStockQuantity(), r.getUserId(), r.getOrderId(), "Reservation expired — stock released"));
        }
        log.info("Released {} expired reservations", expired.size());
    }

    // ============ Helpers ============

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private void logHistory(Product product, StockHistory.ChangeType type, int change, int stockAfter,
                            Long userId, Long orderId, String note) {
        stockHistoryRepository.save(StockHistory.builder()
                .productId(product.getId())
                .productName(product.getName())
                .changeType(type)
                .quantityChange(change)
                .stockAfter(stockAfter)
                .userId(userId)
                .orderId(orderId)
                .note(note)
                .build());
    }

    private StockInfoResponse toStockInfo(Product p) {
        return toStockInfo(p, false);
    }

    private StockInfoResponse toStockInfo(Product p, boolean includeDetails) {
        int stockQty = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
        int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 5;
        long reserved = 0L;
        try {
            Long reservedRaw = reservationRepository.sumActiveReservedQuantity(p.getId());
            reserved = reservedRaw != null ? reservedRaw : 0L;
        } catch (Exception e) {
            log.warn("Reservation lookup failed for product {}: {}", p.getId(), e.getMessage());
        }
        long available = Math.max(0L, stockQty - reserved);
        String category = p.getCategory() != null ? p.getCategory().name() : "OTHER";

        StockInfoResponse.StockInfoResponseBuilder builder = StockInfoResponse.builder()
                .productId(p.getId())
                .productName(p.getName())
                .category(category)
                .stockQuantity(stockQty)
                .lowStockThreshold(threshold)
                .reservedQuantity(reserved)
                .availableQuantity(available)
                .lowStock(stockQty <= threshold)
                .outOfStock(stockQty <= 0);

        if (includeDetails) {
            try {
                builder.history(stockHistoryRepository.findByProductIdOrderByCreatedAtDesc(p.getId()).stream()
                        .limit(20)
                        .map(this::toHistoryDto)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                log.warn("Failed loading stock history for product {}: {}", p.getId(), e.getMessage());
                builder.history(List.of());
            }
            try {
                builder.reservations(reservationRepository.findAll().stream()
                        .filter(r -> r.getProductId() != null && r.getProductId().equals(p.getId()))
                        .map(this::toReservationDto)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                log.warn("Failed loading reservations for product {}: {}", p.getId(), e.getMessage());
                builder.reservations(List.of());
            }
        } else {
            builder.history(List.of()).reservations(List.of());
        }

        return builder.build();
    }

    private StockInfoResponse.StockHistoryDto toHistoryDto(StockHistory h) {
        return StockInfoResponse.StockHistoryDto.builder()
                .id(h.getId())
                .productId(h.getProductId())
                .productName(h.getProductName())
                .changeType(h.getChangeType() != null ? h.getChangeType().name() : null)
                .quantityChange(h.getQuantityChange())
                .stockAfter(h.getStockAfter())
                .orderId(h.getOrderId())
                .note(h.getNote())
                .createdAt(h.getCreatedAt())
                .build();
    }

    private StockInfoResponse.StockReservationDto toReservationDto(StockReservation r) {
        return StockInfoResponse.StockReservationDto.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .quantity(r.getQuantity())
                .orderId(r.getOrderId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, StockInfoResponse> getStockMap() {
        return getAllStockInfo().stream()
                .collect(Collectors.toMap(StockInfoResponse::getProductId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getActiveReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == StockReservation.Status.ACTIVE)
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getReservationsByOrder(Long orderId) {
        return reservationRepository.findByOrderId(orderId).stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }
}
