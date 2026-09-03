package org.example.Healthcareplatform.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.inventory.dto.StockInfoResponse;
import org.example.Healthcareplatform.inventory.dto.StockValidateResponse;
import org.example.Healthcareplatform.inventory.entity.StockHistory;
import org.example.Healthcareplatform.inventory.entity.StockReservation;
import org.example.Healthcareplatform.inventory.repository.StockHistoryRepository;
import org.example.Healthcareplatform.inventory.repository.StockReservationRepository;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockReservationRepository reservationRepository;

    public record ReserveItem(Long productId, Integer quantity) {}

    // ============ Stock read ============

    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        Long reserved = reservationRepository.sumActiveReservedQuantity(productId);
        return (int) Math.max(0, product.getStockQuantity() - reserved);
    }

    @Transactional(readOnly = true)
    public StockValidateResponse validateStock(List<ReserveItem> items) {
        List<StockValidateResponse.ValidationError> errors = new java.util.ArrayList<>();
        for (ReserveItem item : items) {
            Product product = productRepository.findById(item.productId()).orElse(null);
            if (product == null) {
                StockValidateResponse.ValidationError err = new StockValidateResponse.ValidationError();
                err.setProductId(item.productId());
                err.setProductName("Unknown");
                err.setRequested(item.quantity());
                err.setAvailable(0);
                err.setMessage("Product not found");
                errors.add(err);
                continue;
            }
            Long activeReserved = reservationRepository.sumActiveReservedQuantity(product.getId());
            long available = product.getStockQuantity() - activeReserved;
            if (available < item.quantity() || product.getStockQuantity() <= 0) {
                StockValidateResponse.ValidationError err = new StockValidateResponse.ValidationError();
                err.setProductId(product.getId());
                err.setProductName(product.getName());
                err.setRequested(item.quantity());
                err.setAvailable(Math.max(0, available));
                err.setMessage(product.getStockQuantity() <= 0
                        ? product.getName() + " is out of stock"
                        : "Only " + available + " units of " + product.getName() + " are available");
                errors.add(err);
            }
        }
        StockValidateResponse response = new StockValidateResponse();
        response.setValid(errors.isEmpty());
        response.setErrors(errors);
        return response;
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse> getAllStockInfo() {
        return productRepository.findAll().stream()
                .map(this::toStockInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockInfoResponse getStockInfo(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return toStockInfo(product);
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() <= p.getLowStockThreshold())
                .map(this::toStockInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockHistoryDto> getHistory(Long productId) {
        return stockHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    // ============ Stock mutations ============

    @Transactional
    public Product increaseStock(Long productId, int quantity, Long userId, String note) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = getProduct(productId);
        int before = product.getStockQuantity();
        product.setStockQuantity(before + quantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.PURCHASE, quantity, before + quantity, userId, null, note);
        log.info("Increased stock for product={} by {} (new stock={})", productId, quantity, before + quantity);
        return product;
    }

    @Transactional
    public Product decreaseStock(Long productId, int quantity, Long userId, String note) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = getProduct(productId);
        if (quantity > product.getStockQuantity()) {
            throw new IllegalStateException("Insufficient stock for product " + product.getName()
                    + " (requested " + quantity + ", available " + product.getStockQuantity() + ")");
        }
        int before = product.getStockQuantity();
        product.setStockQuantity(before - quantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.ADJUSTMENT, -quantity, before - quantity, userId, null, note);
        log.info("Decreased stock for product={} by {} (new stock={})", productId, quantity, before - quantity);
        return product;
    }

    @Transactional
    public Product adjustStock(Long productId, int newQuantity, Long userId, String note) {
        if (newQuantity < 0) throw new IllegalArgumentException("Stock cannot be negative");
        Product product = getProduct(productId);
        int before = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        logHistory(product, StockHistory.ChangeType.ADJUSTMENT, newQuantity - before, newQuantity, userId, null, note);
        log.info("Adjusted stock for product={} from {} to {} — {}", productId, before, newQuantity, note);
        return product;
    }

    // ============ Reservations ============

    @Transactional
    public void reserveStock(Long userId, Long orderId, List<ReserveItem> items) {
        for (ReserveItem item : items) {
            Product product = getProduct(item.productId());
            Long activeReserved = reservationRepository.sumActiveReservedQuantity(product.getId());
            long available = product.getStockQuantity() - activeReserved;
            if (available < item.quantity()) {
                releaseReservations(orderId, "Stock unavailable");
                throw new IllegalStateException("Insufficient stock for " + product.getName()
                        + " (requested " + item.quantity() + ", available " + available + ")");
            }
            StockReservation reservation = StockReservation.builder()
                    .productId(product.getId())
                    .userId(userId)
                    .quantity(item.quantity())
                    .orderId(orderId)
                    .status(StockReservation.Status.ACTIVE)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                    .build();
            reservationRepository.save(reservation);
            int stockAfter = (int) (product.getStockQuantity() - activeReserved - item.quantity());
            logHistory(product, StockHistory.ChangeType.RESERVED, -item.quantity(),
                    stockAfter, userId, orderId,
                    "Reserved for order " + orderId);
        }
        log.info("Reserved stock for order={} with {} items", orderId, items.size());
    }

    @Transactional
    public void confirmReservations(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        List<StockReservation> toConfirm = reservations.stream()
                .filter(r -> r.getStatus() == StockReservation.Status.ACTIVE
                        || r.getStatus() == StockReservation.Status.EXPIRED)
                .toList();
        if (toConfirm.isEmpty()) return;

        for (StockReservation r : toConfirm) {
            Product product = getProduct(r.getProductId());
            if (r.getStatus() == StockReservation.Status.EXPIRED) {
                // Reservation expired — re-validate available stock before deducting
                Long activeReserved = reservationRepository.sumActiveReservedQuantity(product.getId());
                long available = product.getStockQuantity() - activeReserved;
                if (available < r.getQuantity()) {
                    throw new IllegalStateException("Reservation for " + product.getName()
                            + " has expired and stock is no longer available (requested "
                            + r.getQuantity() + ", available " + available + ")");
                }
            } else {
                if (r.getQuantity() > product.getStockQuantity()) {
                    throw new IllegalStateException("Stock already sold out for product " + product.getName());
                }
            }
            int before = product.getStockQuantity();
            product.setStockQuantity(before - r.getQuantity());
            productRepository.save(product);
            r.setStatus(StockReservation.Status.CONFIRMED);
            reservationRepository.save(r);
            logHistory(product, StockHistory.ChangeType.SALE, -r.getQuantity(),
                    before - r.getQuantity(), r.getUserId(), orderId, "Order confirmed — payment received");
        }
        log.info("Confirmed {} reservations for order={} — stock deducted", toConfirm.size(), orderId);
    }

    @Transactional
    public void releaseReservations(Long orderId, String reason) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, StockReservation.Status.ACTIVE);
        if (reservations.isEmpty()) return;
        for (StockReservation r : reservations) {
            r.setStatus(StockReservation.Status.RELEASED);
            reservationRepository.save(r);
            productRepository.findById(r.getProductId()).ifPresent(p ->
                    logHistory(p, StockHistory.ChangeType.RESERVATION_RELEASED, r.getQuantity(),
                            p.getStockQuantity(), r.getUserId(), orderId, reason));
        }
        log.info("Released {} reservations for order={} ({})", reservations.size(), orderId, reason);
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(StockReservation.Status.ACTIVE, Instant.now());
        if (expired.isEmpty()) return;
        for (StockReservation r : expired) {
            r.setStatus(StockReservation.Status.EXPIRED);
            reservationRepository.save(r);
            productRepository.findById(r.getProductId()).ifPresent(p ->
                    logHistory(p, StockHistory.ChangeType.RESERVATION_EXPIRED, r.getQuantity(),
                            p.getStockQuantity(), r.getUserId(), r.getOrderId(), "Reservation expired — stock released"));
        }
        log.info("Released {} expired reservations", expired.size());
    }

    // ============ Helpers ============

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private void logHistory(Product product, StockHistory.ChangeType type, int change, int stockAfter,
                            Long userId, Long orderId, String note) {
        stockHistoryRepository.save(StockHistory.builder()
                .productId(product.getId())
                .productName(product.getName())
                .changeType(type)
                .quantityChange(change)
                .stockAfter(stockAfter)
                .userId(userId)
                .orderId(orderId)
                .note(note)
                .build());
    }

    private StockInfoResponse toStockInfo(Product p) {
        Long reserved = reservationRepository.sumReservedQuantity(p.getId());
        long available = p.getStockQuantity() - reserved;
        StockInfoResponse info = new StockInfoResponse();
        info.setProductId(p.getId());
        info.setProductName(p.getName());
        info.setCategory(p.getCategory().name());
        info.setStockQuantity(p.getStockQuantity());
        info.setLowStockThreshold(p.getLowStockThreshold());
        info.setReservedQuantity(reserved);
        info.setAvailableQuantity(Math.max(0, available));
        info.setLowStock(p.getStockQuantity() <= p.getLowStockThreshold());
        info.setOutOfStock(p.getStockQuantity() <= 0);
        info.setHistory(stockHistoryRepository.findByProductIdOrderByCreatedAtDesc(p.getId()).stream()
                .limit(50).map(this::toHistoryDto).collect(Collectors.toList()));
        info.setReservations(reservationRepository.findAll().stream()
                .filter(r -> r.getProductId().equals(p.getId()))
                .map(this::toReservationDto).collect(Collectors.toList()));
        return info;
    }

    private StockInfoResponse.StockHistoryDto toHistoryDto(StockHistory h) {
        StockInfoResponse.StockHistoryDto dto = new StockInfoResponse.StockHistoryDto();
        dto.setId(h.getId());
        dto.setProductId(h.getProductId());
        dto.setProductName(h.getProductName());
        dto.setChangeType(h.getChangeType());
        dto.setQuantityChange(h.getQuantityChange());
        dto.setStockAfter(h.getStockAfter());
        dto.setOrderId(h.getOrderId());
        dto.setNote(h.getNote());
        dto.setCreatedAt(h.getCreatedAt());
        return dto;
    }

    private StockInfoResponse.StockReservationDto toReservationDto(StockReservation r) {
        StockInfoResponse.StockReservationDto dto = new StockInfoResponse.StockReservationDto();
        dto.setId(r.getId());
        dto.setProductId(r.getProductId());
        dto.setUserId(r.getUserId());
        dto.setQuantity(r.getQuantity());
        dto.setOrderId(r.getOrderId());
        dto.setStatus(r.getStatus());
        dto.setExpiresAt(r.getExpiresAt());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public Map<Long, StockInfoResponse> getStockMap() {
        return getAllStockInfo().stream()
                .collect(Collectors.toMap(StockInfoResponse::getProductId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getActiveReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == StockReservation.Status.ACTIVE)
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockInfoResponse.StockReservationDto> getReservationsByOrder(Long orderId) {
        return reservationRepository.findByOrderId(orderId).stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }
}
