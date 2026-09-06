package org.example.Healthcareplatform.vendor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.order.dto.OrderResponse;
import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.entity.Order.OrderStatus;
import org.example.Healthcareplatform.order.repository.OrderRepository;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.example.Healthcareplatform.vendor.dto.UpdateVendorProfileRequest;
import org.example.Healthcareplatform.vendor.dto.VendorDashboardStats;
import org.example.Healthcareplatform.vendor.dto.VendorProfileResponse;
import org.example.Healthcareplatform.vendor.service.VendorProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorProfileService vendorProfileService;
    private final SecurityContextUtil securityContextUtil;

    // ============ Dashboard Stats ============

    @GetMapping("/dashboard/stats")
    public ResponseEntity<VendorDashboardStats> getDashboardStats() {
        log.info("GET /api/vendor/dashboard/stats");

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shippedOrders = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long ordersToday = orderRepository.countByCreatedAtAfter(startOfDay);
        BigDecimal revenueToday = orderRepository.sumTotalSince(startOfDay);
        BigDecimal totalRevenue = orderRepository.sumTotalByStatus(OrderStatus.DELIVERED);
        long lowStockCount = productRepository.countByStockQuantityLessThanEqual(10);
        long outOfStockCount = productRepository.countByStockQuantity(0);
        long totalCustomers = userRepository.count();

        VendorDashboardStats stats = new VendorDashboardStats();
        stats.setTotalProducts(totalProducts);
        stats.setTotalOrders(totalOrders);
        stats.setPendingOrders(pendingOrders);
        stats.setProcessingOrders(processingOrders);
        stats.setShippedOrders(shippedOrders);
        stats.setDeliveredOrders(deliveredOrders);
        stats.setOrdersToday(ordersToday);
        stats.setRevenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO);
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.setLowStockCount(lowStockCount);
        stats.setOutOfStockCount(outOfStockCount);
        stats.setTotalCustomers(totalCustomers);
        return ResponseEntity.ok(stats);
    }

    // ============ Orders ============

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status) {
        log.info("GET /api/vendor/orders — page={}, size={}, status={}", page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", orderPage.getContent().stream().map(OrderResponse::fromEntity).toList());
        body.put("page", orderPage.getNumber());
        body.put("size", orderPage.getSize());
        body.put("totalElements", orderPage.getTotalElements());
        body.put("totalPages", orderPage.getTotalPages());
        body.put("first", orderPage.isFirst());
        body.put("last", orderPage.isLast());
        return ResponseEntity.ok(body);
    }

    // ============ Update Order Status ============

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        log.info("PUT /api/vendor/orders/{}/status — status={}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        try {
            order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        }

        orderRepository.save(order);
        return ResponseEntity.ok(OrderResponse.fromEntity(order));
    }

    // ============ Ship Order ============

    @PutMapping("/orders/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        String trackingNumber = body.get("trackingNumber");
        log.info("PUT /api/vendor/orders/{}/ship — tracking={}", orderId, trackingNumber);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.CONFIRMED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order must be in CONFIRMED or PROCESSING status to ship"));
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber != null ? trackingNumber.trim() : null);
        orderRepository.save(order);

        return ResponseEntity.ok(OrderResponse.fromEntity(order));
    }

    // ============ Vendor Business Profile (self-service) ============

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        Long userId = securityContextUtil.getCurrentUserId();
        log.info("GET /api/vendor/profile — userId={}", userId);
        try {
            return ResponseEntity.ok(vendorProfileService.getProfile(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateVendorProfileRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        log.info("PUT /api/vendor/profile — userId={}", userId);
        try {
            return ResponseEntity.ok(vendorProfileService.updateProfile(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}