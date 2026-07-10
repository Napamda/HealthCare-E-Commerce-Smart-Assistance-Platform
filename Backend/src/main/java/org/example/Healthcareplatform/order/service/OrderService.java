package org.example.Healthcareplatform.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.cart.entity.CartItem;
import org.example.Healthcareplatform.cart.repository.CartItemRepository;
import org.example.Healthcareplatform.order.dto.OrderRequest;
import org.example.Healthcareplatform.order.dto.OrderResponse;
import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.entity.OrderItem;
import org.example.Healthcareplatform.order.repository.OrderRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = Order.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .userName(user.getFirstName() + " " + user.getLastName())
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingPhone(request.getShippingPhone())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        List<OrderItem> orderItems = cartItems.stream().map(cart -> {
            BigDecimal subtotal = cart.getUnitPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            return OrderItem.builder()
                    .order(order)
                    .productId(cart.getProductId())
                    .productName(cart.getProductName())
                    .productImage(cart.getProductImage())
                    .quantity(cart.getQuantity())
                    .unitPrice(cart.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        order.setItems(orderItems);

        BigDecimal total = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Clear the cart after successful order
        cartItemRepository.deleteByUserId(userId);

        log.info("Order created: id={}, userId={}, total={}, items={}",
                saved.getId(), userId, total, orderItems.size());
        return OrderResponse.fromEntity(saved);
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

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel order that is " + order.getStatus().name().toLowerCase());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: id={}, userId={}", orderId, userId);
        return OrderResponse.fromEntity(saved);
    }
}
