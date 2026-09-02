package org.example.Healthcareplatform.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.common.CurrentUser;
import org.example.Healthcareplatform.order.dto.OrderRequest;
import org.example.Healthcareplatform.order.dto.OrderResponse;
import org.example.Healthcareplatform.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
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

    private Map<String, Object> toPageBody(Page<OrderResponse> orderPage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", orderPage.getContent());
        body.put("page", orderPage.getNumber());
        body.put("size", orderPage.getSize());
        body.put("totalElements", orderPage.getTotalElements());
        body.put("totalPages", orderPage.getTotalPages());
        body.put("first", orderPage.isFirst());
        body.put("last", orderPage.isLast());
        return body;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request, Authentication auth) {
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
        return ResponseEntity.ok(toPageBody(orderService.getUserOrders(userId, page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/orders/{} — userId={}", orderId, userId);
        try {
            return ResponseEntity.ok(orderService.getOrderById(userId, orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/orders/{}/cancel — userId={}", orderId, userId);
        try {
            return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<?> reorder(@PathVariable Long orderId, Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/orders/{}/reorder — userId={}", orderId, userId);
        try {
            orderService.reorder(userId, orderId);
            return ResponseEntity.ok(Map.of("message", "Items added back to your cart"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ Admin endpoints ============

    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/orders/admin/all — page={}, size={}", page, size);
        return ResponseEntity.ok(toPageBody(orderService.getAllOrders(page, size)));
    }

    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        log.info("PUT /api/orders/admin/{}/status — status={}, actor={}", orderId, status, CurrentUser.userId());
        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, CurrentUser.userId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByAdmin(@PathVariable Long orderId) {
        log.info("POST /api/orders/admin/{}/cancel — actor={}", orderId, CurrentUser.userId());
        try {
            return ResponseEntity.ok(orderService.cancelOrderByAdmin(orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
