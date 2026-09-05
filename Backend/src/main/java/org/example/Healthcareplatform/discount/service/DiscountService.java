package org.example.Healthcareplatform.discount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.discount.dto.DiscountRequest;
import org.example.Healthcareplatform.discount.dto.DiscountResponse;
import org.example.Healthcareplatform.discount.entity.DiscountCode;
import org.example.Healthcareplatform.discount.repository.DiscountCodeRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountCodeRepository discountRepository;

    public record DiscountCalculation(String code, BigDecimal discountAmount, BigDecimal discountedSubtotal) {}

    @Profile("!dev")
    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultCodes() {
        try {
            if (discountRepository.count() > 0) return;
        } catch (Exception e) {
            log.warn("Could not check discount codes — table may not exist yet: {}", e.getMessage());
            return;
        }
        try {
            Instant farFuture = Instant.now().plusSeconds(365L * 24 * 3600);
            discountRepository.save(DiscountCode.builder()
                    .code("WELCOME10").type(DiscountCode.Type.PERCENTAGE)
                    .value(new BigDecimal("10")).minOrderAmount(new BigDecimal("20"))
                    .active(true).expiresAt(farFuture).description("10% off your first order").build());
            discountRepository.save(DiscountCode.builder()
                    .code("SAVE5").type(DiscountCode.Type.FIXED_AMOUNT)
                    .value(new BigDecimal("5")).minOrderAmount(new BigDecimal("30"))
                    .active(true).expiresAt(farFuture).description("$5 off orders over $30").build());
            discountRepository.save(DiscountCode.builder()
                    .code("HEALTHY20").type(DiscountCode.Type.PERCENTAGE)
                    .value(new BigDecimal("20")).minOrderAmount(new BigDecimal("50"))
                    .active(true).expiresAt(farFuture).description("20% off wellness orders over $50").build());
            log.info("Seeded {} default discount codes", 3);
        } catch (Exception e) {
            log.warn("Could not seed discount codes: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public DiscountCalculation validate(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Discount code is required");
        }
        DiscountCode discount = discountRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid discount code: " + code));
        if (!discount.getActive()) throw new IllegalArgumentException("Discount code is inactive");
        if (discount.getExpiresAt() != null && discount.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Discount code has expired");
        }
        if (discount.getUsageLimit() != null && discount.getTimesUsed() >= discount.getUsageLimit()) {
            throw new IllegalArgumentException("Discount code usage limit reached");
        }
        if (discount.getMinOrderAmount() != null && subtotal.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Minimum order amount for this code is $" + discount.getMinOrderAmount());
        }

        BigDecimal discountAmount;
        if (discount.getType() == DiscountCode.Type.PERCENTAGE) {
            discountAmount = subtotal.multiply(discount.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = discount.getValue();
        }
        if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;

        return new DiscountCalculation(discount.getCode(), discountAmount, subtotal.subtract(discountAmount));
    }

    @Transactional
    public void markUsed(String code) {
        discountRepository.findByCodeIgnoreCase(code).ifPresent(d -> {
            d.setTimesUsed(d.getTimesUsed() + 1);
            discountRepository.save(d);
        });
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllCodes() {
        return discountRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DiscountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DiscountResponse createCode(DiscountRequest request) {
        if (discountRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Discount code already exists: " + request.getCode());
        }
        DiscountCode.Type type;
        try {
            type = DiscountCode.Type.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid discount type: " + request.getType());
        }
        DiscountCode code = DiscountCode.builder()
                .code(request.getCode().trim().toUpperCase())
                .type(type)
                .value(request.getValue())
                .minOrderAmount(request.getMinOrderAmount())
                .active(request.getActive() != null ? request.getActive() : true)
                .expiresAt(request.getExpiresAt())
                .usageLimit(request.getUsageLimit())
                .description(request.getDescription())
                .timesUsed(0)
                .build();
        return DiscountResponse.fromEntity(discountRepository.save(code));
    }

    @Transactional
    public DiscountResponse updateCode(Long id, DiscountRequest request) {
        DiscountCode code = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found: " + id));
        if (request.getCode() != null && !request.getCode().isBlank()) {
            code.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            code.setType(DiscountCode.Type.valueOf(request.getType().toUpperCase()));
        }
        if (request.getValue() != null) code.setValue(request.getValue());
        if (request.getMinOrderAmount() != null) code.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getActive() != null) code.setActive(request.getActive());
        if (request.getExpiresAt() != null) code.setExpiresAt(request.getExpiresAt());
        if (request.getUsageLimit() != null) code.setUsageLimit(request.getUsageLimit());
        if (request.getDescription() != null) code.setDescription(request.getDescription());
        return DiscountResponse.fromEntity(discountRepository.save(code));
    }

    @Transactional
    public void deleteCode(Long id) {
        DiscountCode code = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found: " + id));
        discountRepository.delete(code);
    }
}
