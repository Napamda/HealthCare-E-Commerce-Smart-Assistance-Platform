package org.example.Healthcareplatform.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.payment.dto.PaymentExecuteRequest;
import org.example.Healthcareplatform.payment.dto.PaymentResponse;
import org.example.Healthcareplatform.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = getUserId(auth);
        Long orderId = ((Number) body.get("orderId")).longValue();
        String method = (String) body.get("method");
        log.info("POST /api/payments/initiate — userId={}, orderId={}, method={}", userId, orderId, method);
        try {
            return ResponseEntity.ok(paymentService.initiatePayment(userId, orderId, method));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/execute/card")
    public ResponseEntity<?> executeCard(@PathVariable Long paymentId,
                                         @Valid @RequestBody PaymentExecuteRequest request,
                                         Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/payments/{}/execute/card — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.executeCardPayment(userId, paymentId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/execute/paypal")
    public ResponseEntity<?> executePayPal(@PathVariable Long paymentId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/payments/{}/execute/paypal — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.executePayPalPayment(userId, paymentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/payments/{} — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.getPayment(userId, paymentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentsForOrder(@PathVariable Long orderId, Authentication auth) {
        Long userId = getUserId(auth);
        try {
            return ResponseEntity.ok(paymentService.getPaymentsForOrder(userId, orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getUserPayments(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(paymentService.getUserPayments(userId));
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<?> getReceipt(@PathVariable String receiptNumber, Authentication auth) {
        Long userId = getUserId(auth);
        try {
            return ResponseEntity.ok(paymentService.getPaymentByReceipt(userId, receiptNumber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
