package org.example.Healthcareplatform.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.order.dto.OrderRequest;
import org.example.Healthcareplatform.order.dto.OrderResponse;
import org.example.Healthcareplatform.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/orders — userId={}", userId);

        try {
            OrderResponse response = orderService.createOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/orders — userId={}, page={}, size={}", userId, page, size);

        Page<OrderResponse> orderPage = orderService.getUserOrders(userId, page, size);

        Map<String, Object> body = Map.of(
                "content", orderPage.getContent(),
                "page", orderPage.getNumber(),
                "size", orderPage.getSize(),
                "totalElements", orderPage.getTotalElements(),
                "totalPages", orderPage.getTotalPages(),
                "first", orderPage.isFirst(),
                "last", orderPage.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long orderId,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/orders/{} — userId={}", orderId, userId);

        try {
            OrderResponse response = orderService.getOrderById(userId, orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/orders/{}/cancel — userId={}", orderId, userId);

        try {
            OrderResponse response = orderService.cancelOrder(userId, orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
