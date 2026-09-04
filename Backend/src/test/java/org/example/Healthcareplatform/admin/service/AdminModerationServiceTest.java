package org.example.Healthcareplatform.admin.service;

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
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.entity.UserStatus;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private VendorProfileRepository vendorProfileRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private HealthEventRepository healthEventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminModerationService moderationService;

    private static final Long ADMIN_ID = 99L;

    // ---- Task 3.2 — Vendor Management ----

    @Test
    void getVendorsFiltersByStatus() {
        VendorProfile profile = VendorProfile.builder()
                .id(1L).userId(5L).businessName("Med Supply Co")
                .approvalStatus(VendorProfile.ApprovalStatus.PENDING).build();
        when(vendorProfileRepository.findByApprovalStatus(VendorProfile.ApprovalStatus.PENDING))
                .thenReturn(List.of(profile));
        when(userRepository.findById(5L)).thenReturn(Optional.of(
                User.builder().id(5L).email("vendor@test.com")
                        .firstName("Vendor").lastName("One")
                        .role(UserRole.VENDOR).build()));

        List<VendorResponse> result = moderationService.getVendors("PENDING");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessName()).isEqualTo("Med Supply Co");
        assertThat(result.get(0).getApprovalStatus()).isEqualTo("PENDING");
    }

    @Test
    void approveVendorSetsStatusAndReviewer() {
        VendorProfile profile = VendorProfile.builder()
                .id(1L).userId(5L).businessName("Med Supply Co")
                .approvalStatus(VendorProfile.ApprovalStatus.PENDING).build();
        when(vendorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(vendorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("APPROVED");

        VendorResponse result = moderationService.decideOnVendor(ADMIN_ID, 1L, req);

        assertThat(result.getApprovalStatus()).isEqualTo("APPROVED");
        assertThat(result.getReviewedAt()).isNotNull();
        verify(vendorProfileRepository).save(any());
    }

    @Test
    void rejectVendorRequiresReasonAndSuspendsUser() {
        VendorProfile profile = VendorProfile.builder()
                .id(1L).userId(5L).businessName("Bad Co")
                .approvalStatus(VendorProfile.ApprovalStatus.PENDING).build();
        User vendor = User.builder()
                .id(5L).email("bad@test.com").role(UserRole.VENDOR)
                .status(UserStatus.ACTIVE).build();
        when(vendorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(5L)).thenReturn(Optional.of(vendor));
        when(vendorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("REJECTED");
        req.setReason("Invalid business license");

        VendorResponse result = moderationService.decideOnVendor(ADMIN_ID, 1L, req);

        assertThat(result.getApprovalStatus()).isEqualTo("REJECTED");
        assertThat(result.getRejectionReason()).isEqualTo("Invalid business license");
        assertThat(vendor.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(vendor.getRefreshToken()).isNull();
        verify(userRepository).save(vendor);
    }

    @Test
    void rejectVendorWithoutReasonThrows() {
        VendorProfile profile = VendorProfile.builder()
                .id(1L).userId(5L)
                .approvalStatus(VendorProfile.ApprovalStatus.PENDING).build();
        when(vendorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("REJECTED");

        assertThatThrownBy(() -> moderationService.decideOnVendor(ADMIN_ID, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void decideOnAlreadyDecidedVendorThrows() {
        VendorProfile profile = VendorProfile.builder()
                .id(1L).userId(5L)
                .approvalStatus(VendorProfile.ApprovalStatus.APPROVED).build();
        when(vendorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> moderationService.decideOnVendor(ADMIN_ID, 1L, new ModerationDecisionRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been");
    }

    // ---- Task 3.3 — Product Moderation ----

    @Test
    void getProductsReturnsModerationQueue() {
        Product pending = Product.builder()
                .id(1L).name("Vitamin C").price(BigDecimal.TEN)
                .category(Product.ProductCategory.VITAMINS)
                .status(ProductStatus.PENDING).build();
        when(productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.PENDING))
                .thenReturn(List.of(pending));
        when(productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.REJECTED))
                .thenReturn(List.of());

        List<ModerationProductResponse> result = moderationService.getProductsByStatus(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void approveProductSetsApproved() {
        Product product = Product.builder()
                .id(1L).name("Vitamin C").price(BigDecimal.TEN)
                .category(Product.ProductCategory.VITAMINS)
                .status(ProductStatus.PENDING).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("APPROVED");

        ModerationProductResponse result = moderationService.decideOnProduct(ADMIN_ID, 1L, req);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(product.getModeratedBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    void rejectProductRequiresReason() {
        Product product = Product.builder()
                .id(1L).name("Bad Drug").price(BigDecimal.ONE)
                .category(Product.ProductCategory.OTHER)
                .status(ProductStatus.PENDING).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("REJECTED");

        assertThatThrownBy(() -> moderationService.decideOnProduct(ADMIN_ID, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void decideOnAlreadyModeratedProductThrows() {
        Product product = Product.builder()
                .id(1L).name("Test").price(BigDecimal.ONE)
                .category(Product.ProductCategory.OTHER)
                .status(ProductStatus.APPROVED).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> moderationService.decideOnProduct(ADMIN_ID, 1L, new ModerationDecisionRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been");
    }

    // ---- Task 3.4 — Event Moderation ----

    @Test
    void getEventsReturnsPendingByDefault() {
        HealthEvent event = HealthEvent.builder()
                .id(1L).title("Health Camp").category("CHECKUP")
                .status(EventStatus.PENDING).build();
        when(healthEventRepository.findByStatusOrderByCreatedAtDesc(EventStatus.PENDING))
                .thenReturn(List.of(event));

        List<ModerationEventResponse> result = moderationService.getEventsByStatus(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void approveEventPublishesIt() {
        HealthEvent event = HealthEvent.builder()
                .id(1L).title("Health Camp").category("CHECKUP")
                .status(EventStatus.PENDING).build();
        when(healthEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(healthEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("APPROVED");

        ModerationEventResponse result = moderationService.decideOnEvent(ADMIN_ID, 1L, req);

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getModeratedBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    void rejectEventCancelsWithReason() {
        HealthEvent event = HealthEvent.builder()
                .id(1L).title("Bad Event").category("OTHER")
                .status(EventStatus.PENDING).build();
        when(healthEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(healthEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("REJECTED");
        req.setReason("Misleading description");

        ModerationEventResponse result = moderationService.decideOnEvent(ADMIN_ID, 1L, req);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getModerationReason()).isEqualTo("Misleading description");
    }

    @Test
    void rejectEventWithoutReasonThrows() {
        HealthEvent event = HealthEvent.builder()
                .id(1L).title("Bad Event").category("OTHER")
                .status(EventStatus.PENDING).build();
        when(healthEventRepository.findById(1L)).thenReturn(Optional.of(event));

        ModerationDecisionRequest req = new ModerationDecisionRequest();
        req.setDecision("REJECTED");

        assertThatThrownBy(() -> moderationService.decideOnEvent(ADMIN_ID, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }
}
