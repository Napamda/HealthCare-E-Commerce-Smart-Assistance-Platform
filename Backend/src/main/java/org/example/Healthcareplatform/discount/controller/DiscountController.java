package org.example.Healthcareplatform.discount.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.discount.dto.DiscountRequest;
import org.example.Healthcareplatform.discount.dto.DiscountResponse;
import org.example.Healthcareplatform.discount.service.DiscountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String code, @RequestParam BigDecimal subtotal) {
        log.info("GET /api/discounts/validate — code={}, subtotal={}", code, subtotal);
        DiscountService.DiscountCalculation calc = discountService.validate(code, subtotal);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", calc.code(),
                "discountAmount", calc.discountAmount(),
                "discountedSubtotal", calc.discountedSubtotal()
        ));
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> getAllCodes() {
        return ResponseEntity.ok(discountService.getAllCodes());
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> createCode(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.createCode(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> updateCode(@PathVariable Long id,
                                                       @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.updateCode(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCode(@PathVariable Long id) {
        discountService.deleteCode(id);
        return ResponseEntity.noContent().build();
    }
}
