package org.example.Healthcareplatform.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.cart.entity.CartItem;
import org.example.Healthcareplatform.cart.repository.CartItemRepository;
import org.example.Healthcareplatform.discount.service.DiscountService;
import org.example.Healthcareplatform.inventory.service.InventoryService;
import org.example.Healthcareplatform.discount.service.DiscountService;
import org.example.Healthcareplatform.inventory.service.InventoryService;
import org.example.Healthcareplatform.order.dto.CheckoutPreviewRequest;
import org.example.Healthcareplatform.order.dto.CheckoutPreviewResponse;
import org.example.Healthcareplatform.order.dto.OrderRequest;
import org.example.Healthcareplatform.order.dto.OrderResponse;
import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.entity.OrderItem;
import org.example.Healthcareplatform.order.repository.OrderRepository;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DiscountService discountService;

    private static final List<String> SHIPPING_METHODS = List.of("STANDARD", "EXPRESS", "SAME_DAY");

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        String shippingMethod = resolveShippingMethod(request.getShippingMethod());

        // --- Validate stock availability (available = stock - reserved) ---
        List<InventoryService.ReserveItem> reserveItems = cartItems.stream()
                .map(c -> new InventoryService.ReserveItem(c.getProductId(), c.getQuantity()))
                .collect(Collectors.toList());

        // --- Prescription requirement validation ---
        boolean hasPrescriptionRequired = cartItems.stream().anyMatch(c -> {
            Product product = productRepository.findById(c.getProductId()).orElse(null);
            return product != null && Boolean.TRUE.equals(product.getPrescriptionRequired());
        });
        if (hasPrescriptionRequired && !Boolean.TRUE.equals(request.getConfirmPrescription())) {
            throw new IllegalArgumentException(
                    "This order contains prescription-only products. Please confirm you have a valid prescription.");
        }

        // --- Compute subtotal ---
        BigDecimal subtotal = cartItems.stream()
                .map(c -> c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Apply discount code ---
        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountCode = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            DiscountService.DiscountCalculation calc = discountService.validate(request.getDiscountCode(), subtotal);
            discountAmount = calc.discountAmount();
            discountCode = calc.code();
        }

        // --- Shipping cost ---
        BigDecimal shippingAmount = computeShipping(shippingMethod, subtotal.subtract(discountAmount));

        // --- Tax on discounted subtotal ---
        BigDecimal taxable = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = taxable.add(shippingAmount).add(taxAmount);

        // --- Persist order ---
        Order order = Order.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .userName(user.getFirstName() + " " + user.getLastName())
                .orderNumber(generateOrderNumber())
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingPhone(request.getShippingPhone())
                .shippingMethod(shippingMethod)
                .paymentMethod(request.getPaymentMethod())
                .discountCode(discountCode)
                .subtotalAmount(subtotal)
                .discountAmount(discountAmount)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .totalAmount(total)
                .notes(request.getNotes())
                .build();

        List<OrderItem> orderItems = cartItems.stream().map(cart -> {
            BigDecimal itemSubtotal = cart.getUnitPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            return OrderItem.builder()
                    .order(order)
                    .productId(cart.getProductId())
                    .productName(cart.getProductName())
                    .productImage(cart.getProductImage())
                    .quantity(cart.getQuantity())
                    .unitPrice(cart.getUnitPrice())
                    .subtotal(itemSubtotal)
                    .build();
        }).collect(Collectors.toList());
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        // --- Reserve stock for the order ---
        inventoryService.reserveStock(userId, saved.getId(), reserveItems);

        // Clear the cart after successful order creation
        cartItemRepository.deleteByUserId(userId);

        if (discountCode != null) {
            discountService.markUsed(discountCode);
        }

        log.info("Order created: number={}, id={}, userId={}, total={}, items={}",
                saved.getOrderNumber(), saved.getId(), userId, total, orderItems.size());
        return OrderResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(Long userId, CheckoutPreviewRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        String shippingMethod = resolveShippingMethod(request.getShippingMethod());

        // Subtotal
        BigDecimal subtotal = cartItems.stream()
                .map(c -> c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountCode = null;
        boolean discountValid = false;
        String discountError = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            try {
                DiscountService.DiscountCalculation calc = discountService.validate(request.getDiscountCode(), subtotal);
                discountAmount = calc.discountAmount();
                discountCode = calc.code();
                discountValid = true;
            } catch (IllegalArgumentException e) {
                discountError = e.getMessage();
            }
        }

        // Shipping
        BigDecimal shippingAmount = computeShipping(shippingMethod, subtotal.subtract(discountAmount));

        // Tax
        BigDecimal taxable = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = taxable.add(shippingAmount).add(taxAmount);

        // Prescription check
        boolean hasPrescriptionRequired = cartItems.stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getPrescriptionRequired()));

        int itemCount = cartItems.stream().mapToInt(CartItem::getQuantity).sum();

        return CheckoutPreviewResponse.builder()
                .subtotalAmount(subtotal)
                .discountAmount(discountAmount)
                .discountedSubtotal(taxable)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .totalAmount(total)
                .shippingMethod(shippingMethod)
                .discountCode(discountCode)
                .hasPrescriptionRequired(hasPrescriptionRequired)
                .discountValid(discountValid)
                .discountError(discountError)
                .itemCount(itemCount)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumberForAdmin(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return doCancel(order);
    }

    @Transactional
    public OrderResponse cancelOrderByAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return doCancel(order);
    }

    private OrderResponse doCancel(Order order) {
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED ||
                order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel order that is " + order.getStatus().name().toLowerCase());
        }

        Order.OrderStatus previous = order.getStatus();
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Restore stock: if already confirmed, stock was deducted -> add it back; release reservations
        inventoryService.releaseReservations(order.getId(), "Order cancelled");
        if (previous == Order.OrderStatus.CONFIRMED ||
                previous == Order.OrderStatus.PROCESSING) {
            for (OrderItem item : saved.getItems()) {
                inventoryService.increaseStock(item.getProductId(), item.getQuantity(),
                        order.getUserId(), "Stock restored — order " + order.getOrderNumber() + " cancelled");
            }
        }
        log.info("Order cancelled: number={}, id={}", saved.getOrderNumber(), saved.getId());
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus, Long actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Order.OrderStatus target;
        try {
            target = Order.OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot change status of a " + order.getStatus().name().toLowerCase() + " order");
        }

        if (target == Order.OrderStatus.CANCELLED) {
            return doCancel(order);
        }

        // Validate workflow: PENDING -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
        List<Order.OrderStatus> flow = List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.PROCESSING,
                Order.OrderStatus.SHIPPED,
                Order.OrderStatus.DELIVERED);
        int currentIdx = flow.indexOf(order.getStatus());
        int targetIdx = flow.indexOf(target);
        if (targetIdx < currentIdx || targetIdx > currentIdx + 1) {
            throw new IllegalArgumentException("Invalid status transition from "
                    + order.getStatus() + " to " + target);
        }

        order.setStatus(target);
        Order saved = orderRepository.save(order);
        log.info("Order {} status updated: {} -> {} by user {}", saved.getOrderNumber(), order.getStatus(), target, actorUserId);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public void reorder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        int added = 0;
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null || product.getStockQuantity() <= 0) continue;

            int qty = Math.min(item.getQuantity(), product.getStockQuantity());
            cartItemRepository.findByUserIdAndProductId(userId, product.getId())
                    .ifPresentOrElse(existing -> {
                        int newQty = existing.getQuantity() + qty;
                        if (newQty > product.getStockQuantity()) newQty = product.getStockQuantity();
                        existing.setQuantity(newQty);
                        cartItemRepository.save(existing);
                    }, () -> cartItemRepository.save(CartItem.builder()
                            .userId(userId)
                            .productId(product.getId())
                            .productName(product.getName())
                            .productImage(product.getImageUrl())
                            .quantity(qty)
                            .unitPrice(product.getPrice())
                            .build()));
            added += qty;
        }
        if (added == 0) {
            throw new IllegalStateException("No items could be added to cart — items are out of stock");
        }
        log.info("Reorder completed for order={}, user={}, items added={}", orderId, userId, added);
    }

    private String resolveShippingMethod(String method) {
        if (method == null || method.isBlank()) return "STANDARD";
        String upper = method.toUpperCase();
        return SHIPPING_METHODS.contains(upper) ? upper : "STANDARD";
    }

    private BigDecimal computeShipping(String method, BigDecimal orderTotal) {
        return switch (method) {
            case "EXPRESS" -> new BigDecimal("9.99");
            case "SAME_DAY" -> new BigDecimal("14.99");
            default -> orderTotal.compareTo(new BigDecimal("50")) >= 0
                    ? BigDecimal.ZERO
                    : new BigDecimal("4.99");
        };
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 10; attempt++) {
            int suffix = 100000 + RANDOM.nextInt(900000);
            String number = "HC" + datePart + "-" + suffix;
            if (orderRepository.findByOrderNumber(number).isEmpty()) {
                return number;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number");
    }
}
