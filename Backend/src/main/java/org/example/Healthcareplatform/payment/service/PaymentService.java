package org.example.Healthcareplatform.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.inventory.service.InventoryService;
import org.example.Healthcareplatform.notification.service.NotificationService;
import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.repository.OrderRepository;
import org.example.Healthcareplatform.payment.dto.PaymentExecuteRequest;
import org.example.Healthcareplatform.payment.dto.PaymentResponse;
import org.example.Healthcareplatform.payment.entity.Payment;
import org.example.Healthcareplatform.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse initiatePayment(Long userId, Long orderId, String methodName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        Payment.Method method;
        try {
            method = Payment.Method.valueOf(methodName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment method: " + methodName);
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay for a cancelled order");
        }
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Order is already " + order.getStatus().name().toLowerCase());
        }
        if (order.getStatus() == Order.OrderStatus.CONFIRMED ||
                order.getStatus() == Order.OrderStatus.PROCESSING) {
            if (paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, Payment.Status.SUCCESS).isPresent()) {
                throw new IllegalArgumentException("Order has already been paid");
            }
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(userId)
                .amount(order.getTotalAmount())
                .method(method)
                .status(Payment.Status.PENDING)
                .build();

        if (method == Payment.Method.CASH_ON_DELIVERY) {
            markSuccessful(payment, null);
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }
            inventoryService.confirmReservations(order.getId());
            notificationService.notify(userId, "ORDER", "Order confirmed",
                    "Your order " + order.getOrderNumber() + " is confirmed. You will pay "
                            + order.getTotalAmount() + " on delivery.");
        } else {
            Payment saved = paymentRepository.save(payment);
            log.info("Payment initiated: id={}, order={}, method={}", saved.getId(), order.getOrderNumber(), method);
            notificationService.notify(userId, "PAYMENT", "Payment pending",
                    "A payment of $" + order.getTotalAmount() + " for order " + order.getOrderNumber()
                            + " is awaiting completion.");
            return PaymentResponse.fromEntity(saved);
        }
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentResponse executeCardPayment(Long userId, Long paymentId, PaymentExecuteRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment does not belong to user");
        }
        if (payment.getStatus() == Payment.Status.SUCCESS) {
            throw new IllegalArgumentException("Payment is already successful");
        }
        if (payment.getMethod() != Payment.Method.CARD) {
            throw new IllegalArgumentException("This payment is not a card payment");
        }

        String declineCode = simulateGateway(request);
        if (declineCode != null) {
            payment.setStatus(Payment.Status.FAILED);
            payment.setErrorMessage(declineCode);
            payment.setBillingName(request.getCardHolder());
            paymentRepository.save(payment);
            log.warn("Card payment FAILED: payment={}, reason={}", paymentId, declineCode);
            notificationService.notify(userId, "PAYMENT", "Payment failed",
                    "Your payment for order " + payment.getOrderNumber() + " was declined (" + declineCode + ").");
            return PaymentResponse.fromEntity(payment);
        }

        markSuccessful(payment, request.getCardHolder());
        confirmOrderStock(payment);
        notificationService.notify(userId, "PAYMENT", "Payment successful",
                "Payment of $" + payment.getAmount() + " for order " + payment.getOrderNumber()
                        + " was successful. Receipt: " + payment.getReceiptNumber());
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentResponse executePayPalPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment does not belong to user");
        }
        if (payment.getStatus() == Payment.Status.SUCCESS) {
            throw new IllegalArgumentException("Payment is already successful");
        }
        if (payment.getMethod() != Payment.Method.PAYPAL) {
            throw new IllegalArgumentException("This payment is not a PayPal payment");
        }

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        markSuccessful(payment, null);
        confirmOrderStock(payment);
        notificationService.notify(userId, "PAYMENT", "Payment successful",
                "PayPal payment of $" + payment.getAmount() + " for order " + payment.getOrderNumber()
                        + " was successful. Receipt: " + payment.getReceiptNumber());
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment does not belong to user");
        }
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReceipt(Long userId, String receiptNumber) {
        Payment payment = paymentRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment does not belong to user");
        }
        return PaymentResponse.fromEntity(payment);
    }

    private String simulateGateway(PaymentExecuteRequest request) {
        String digits = request.getCardNumber().replaceAll("\\s+", "");
        if (!luhnCheck(digits)) {
            return "Card number failed checksum validation";
        }
        if (digits.endsWith("0002")) {
            return "Card declined by the issuing bank";
        }
        int expiryMonth = Integer.parseInt(request.getExpiryMonth());
        int expiryYear = Integer.parseInt(request.getExpiryYear());
        LocalDate now = LocalDate.now();
        if (expiryYear < now.getYear() ||
                (expiryYear == now.getYear() && expiryMonth < now.getMonthValue())) {
            return "Card has expired";
        }
        return null;
    }

    private boolean luhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void markSuccessful(Payment payment, String billingName) {
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setTransactionId("TXN" + System.currentTimeMillis() + RANDOM.nextInt(1000));
        payment.setReceiptNumber("RCPT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (10000 + RANDOM.nextInt(90000)));
        payment.setPaidAt(Instant.now());
        payment.setBillingName(billingName);
        payment.setErrorMessage(null);
        paymentRepository.save(payment);
    }

    private void confirmOrderStock(Payment payment) {
        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        inventoryService.confirmReservations(payment.getOrderId());
        log.info("Payment confirmed — order {} marked CONFIRMED, stock deducted", payment.getOrderNumber());
    }
}
