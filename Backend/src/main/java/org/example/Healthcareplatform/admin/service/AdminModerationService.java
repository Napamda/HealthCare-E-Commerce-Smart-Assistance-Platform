package org.example.Healthcareplatform.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.admin.dto.ModerationDecisionRequest;
import org.example.Healthcareplatform.admin.dto.ModerationEventResponse;
import org.example.Healthcareplatform.admin.dto.ModerationProductResponse;
import org.example.Healthcareplatform.admin.dto.VendorResponse;
import org.example.Healthcareplatform.admin.entity.VendorProfile;
import org.example.Healthcareplatform.admin.repository.VendorProfileRepository;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.entity.ProductStatus;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminModerationService {

    private final VendorProfileRepository vendorProfileRepository;
    private final ProductRepository productRepository;
    private final HealthEventRepository healthEventRepository;
    private final UserRepository userRepository;

    // ===============================================================
    // Task 3.2 — Vendor Management
    // ===============================================================

    @Transactional(readOnly = true)
    public List<VendorResponse> getVendors(String statusFilter) {
        List<VendorProfile> profiles;
        if (statusFilter != null && !statusFilter.isBlank()) {
            VendorProfile.ApprovalStatus filter = parseApprovalStatus(statusFilter);
            profiles = vendorProfileRepository.findByApprovalStatus(filter);
        } else {
            profiles = vendorProfileRepository
                    .findByApprovalStatusInOrderByCreatedAtDesc(List.of(
                            VendorProfile.ApprovalStatus.PENDING,
                            VendorProfile.ApprovalStatus.APPROVED,
                            VendorProfile.ApprovalStatus.REJECTED));
        }
        return profiles.stream().map(this::toVendorResponse).collect(Collectors.toList());
    }

    @Transactional
    public VendorResponse decideOnVendor(Long adminId, Long profileId, ModerationDecisionRequest request) {
        VendorProfile profile = vendorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor application not found with id: " + profileId));

        if (profile.getApprovalStatus() != VendorProfile.ApprovalStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Vendor application has already been " + profile.getApprovalStatus().name().toLowerCase());
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        String reason = request.getReason() == null ? null : request.getReason().trim();

        if ("APPROVED".equals(decision)) {
            profile.setApprovalStatus(VendorProfile.ApprovalStatus.APPROVED);
            profile.setRejectionReason(null);
            log.info("Vendor approved — adminId={}, profileId={}, userId={}", adminId, profileId, profile.getUserId());
        } else if ("REJECTED".equals(decision)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("A reason is required when rejecting a vendor application");
            }
            profile.setApprovalStatus(VendorProfile.ApprovalStatus.REJECTED);
            profile.setRejectionReason(reason);
            // Suspend the user so they can't access vendor features after rejection
            userRepository.findById(profile.getUserId()).ifPresent(user -> {
                user.setStatus(org.example.Healthcareplatform.user.entity.UserStatus.SUSPENDED);
                user.setSuspendedReason("Vendor application rejected: " + reason);
                user.setSuspendedAt(LocalDateTime.now());
                user.setRefreshToken(null);
                userRepository.save(user);
            });
            log.info("Vendor rejected — adminId={}, profileId={}, userId={}, reason={}",
                    adminId, profileId, profile.getUserId(), reason);
        } else {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }

        profile.setReviewedBy(adminId);
        profile.setReviewedAt(LocalDateTime.now());
        return toVendorResponse(vendorProfileRepository.save(profile));
    }

    // ===============================================================
    // Task 3.3 — Product Moderation
    // ===============================================================

    @Transactional(readOnly = true)
    public List<ModerationProductResponse> getProductsByStatus(String status) {
        ProductStatus filter = parseProductStatus(status);
        if (filter == null) {
            // Return all pending + rejected (moderation queue)
            List<Product> queue = new java.util.ArrayList<>(
                    productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.PENDING));
            queue.addAll(productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.REJECTED));
            return queue.stream().map(this::toProductResponse).collect(Collectors.toList());
        }
        return productRepository.findByStatusOrderByCreatedAtDesc(filter)
                .stream().map(this::toProductResponse).collect(Collectors.toList());
    }

    @Transactional
    public ModerationProductResponse decideOnProduct(Long adminId, Long productId,
                                                      ModerationDecisionRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        if (product.getStatus() != ProductStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Product has already been " + product.getStatus().name().toLowerCase());
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        String reason = request.getReason() == null ? null : request.getReason().trim();

        if ("APPROVED".equals(decision)) {
            product.setStatus(ProductStatus.APPROVED);
            product.setModerationReason(null);
            log.info("Product approved — adminId={}, productId={}", adminId, productId);
        } else if ("REJECTED".equals(decision)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("A reason is required when rejecting a product");
            }
            product.setStatus(ProductStatus.REJECTED);
            product.setModerationReason(reason);
            log.info("Product rejected — adminId={}, productId={}, reason={}", adminId, productId, reason);
        } else {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }

        product.setModeratedBy(adminId);
        return toProductResponse(productRepository.save(product));
    }

    // ===============================================================
    // Task 3.4 — Event Moderation
    // ===============================================================

    @Transactional(readOnly = true)
    public List<ModerationEventResponse> getEventsByStatus(String status) {
        EventStatus filter = parseEventStatus(status);
        if (filter == null) {
            // Default: show pending events
            return healthEventRepository.findByStatusOrderByCreatedAtDesc(EventStatus.PENDING)
                    .stream().map(this::toEventResponse).collect(Collectors.toList());
        }
        return healthEventRepository.findByStatusOrderByCreatedAtDesc(filter)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    @Transactional
    public ModerationEventResponse decideOnEvent(Long adminId, Long eventId,
                                                  ModerationDecisionRequest request) {
        HealthEvent event = healthEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Event has already been moderated (status: " + event.getStatus().name() + ")");
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        String reason = request.getReason() == null ? null : request.getReason().trim();

        if ("APPROVED".equals(decision) || "PUBLISHED".equals(decision)) {
            event.setStatus(EventStatus.PUBLISHED);
            event.setModerationReason(null);
            log.info("Event approved — adminId={}, eventId={}", adminId, eventId);
        } else if ("REJECTED".equals(decision) || "CANCELLED".equals(decision)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("A reason is required when rejecting an event");
            }
            event.setStatus(EventStatus.CANCELLED);
            event.setModerationReason(reason);
            log.info("Event rejected — adminId={}, eventId={}, reason={}", adminId, eventId, reason);
        } else {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }

        event.setModeratedBy(adminId);
        return toEventResponse(healthEventRepository.save(event));
    }

    // ===============================================================
    // Helpers
    // ===============================================================

    private VendorResponse toVendorResponse(VendorProfile p) {
        Optional<User> user = userRepository.findById(p.getUserId());
        return VendorResponse.builder()
                .profileId(p.getId())
                .userId(p.getUserId())
                .email(user.map(User::getEmail).orElse("(unknown)"))
                .firstName(user.map(User::getFirstName).orElse(""))
                .lastName(user.map(User::getLastName).orElse(""))
                .businessName(p.getBusinessName())
                .businessLicense(p.getBusinessLicense())
                .approvalStatus(p.getApprovalStatus().name())
                .rejectionReason(p.getRejectionReason())
                .reviewedAt(p.getReviewedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ModerationProductResponse toProductResponse(Product p) {
        return ModerationProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory().name())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .manufacturer(p.getManufacturer())
                .prescriptionRequired(p.getPrescriptionRequired())
                .status(p.getStatus().name())
                .moderationReason(p.getModerationReason())
                .moderatedBy(p.getModeratedBy())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ModerationEventResponse toEventResponse(HealthEvent e) {
        return ModerationEventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .category(e.getCategory())
                .organizer(e.getOrganizer())
                .city(e.getCity())
                .status(e.getStatus().name())
                .moderationReason(e.getModerationReason())
                .moderatedBy(e.getModeratedBy())
                .startDateTime(e.getStartDateTime())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private VendorProfile.ApprovalStatus parseApprovalStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return VendorProfile.ApprovalStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid vendor status: " + s);
        }
    }

    private ProductStatus parseProductStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ProductStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid product status: " + s);
        }
    }

    private EventStatus parseEventStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return EventStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid event status: " + s);
        }
    }
}
