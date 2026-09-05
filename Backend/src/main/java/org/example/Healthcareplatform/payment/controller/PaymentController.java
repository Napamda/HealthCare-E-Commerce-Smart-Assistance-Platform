package org.example.Healthcareplatform.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.payment.dto.PaymentExecuteRequest;
import org.example.Healthcareplatform.payment.dto.PaymentInitiateRequest;
import org.example.Healthcareplatform.payment.dto.PaymentMethodResponse;
import org.example.Healthcareplatform.payment.dto.PaymentReceiptDTO;
import org.example.Healthcareplatform.payment.dto.PaymentResponse;
import org.example.Healthcareplatform.payment.dto.PaymentValidationRequest;
import org.example.Healthcareplatform.payment.dto.PaymentValidationResponse;
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
    public ResponseEntity<?> initiate(@Valid @RequestBody PaymentInitiateRequest request, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/payments/initiate — userId={}, orderId={}, method={}",
                userId, request.getOrderId(), request.getMethod());
        try {
            return ResponseEntity.ok(paymentService.initiatePayment(userId, request.getOrderId(), request.getMethod()));
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/execute/paypal")
    public ResponseEntity<?> executePayPal(@PathVariable Long paymentId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/payments/{}/execute/paypal — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.executePayPalPayment(userId, paymentId));
        } catch (IllegalArgumentException | IllegalStateException e) {
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

    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodResponse>> getAvailablePaymentMethods() {
        log.info("GET /api/payments/methods");
        return ResponseEntity.ok(paymentService.getAvailablePaymentMethods());
    }

    @PostMapping("/validate")
    public ResponseEntity<PaymentValidationResponse> validatePayment(
            @Valid @RequestBody PaymentValidationRequest request) {
        log.info("POST /api/payments/validate — amount={}, method={}",
                request.getAmount(), request.getPaymentMethod());
        return ResponseEntity.ok(paymentService.validatePayment(request));
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<?> generateReceipt(@PathVariable Long paymentId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/payments/{}/receipt — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.generateReceipt(userId, paymentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<?> retryPayment(@PathVariable Long paymentId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/payments/{}/retry — user={}", paymentId, userId);
        try {
            return ResponseEntity.ok(paymentService.retryPayment(userId, paymentId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
