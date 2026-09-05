package org.example.Healthcareplatform.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.cart.dto.CartMergeItem;
import org.example.Healthcareplatform.cart.dto.CartMergeItem;
import org.example.Healthcareplatform.cart.dto.CartRequest;
import org.example.Healthcareplatform.cart.dto.CartResponse;
import org.example.Healthcareplatform.cart.entity.CartItem;
import org.example.Healthcareplatform.cart.repository.CartItemRepository;
import org.example.Healthcareplatform.inventory.service.InventoryService;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<CartResponse> getCart(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCartSummary(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CartResponse> cartItems = items.stream()
                .map(CartResponse::fromEntity)
                .collect(Collectors.toList());

        int itemCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        var total = items.stream()
                .map(i -> i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return Map.of(
                "items", cartItems,
                "itemCount", itemCount,
                "total", total
        );
    }

    @Transactional
    public Map<String, Object> mergeCartItems(Long userId, List<CartMergeItem> items) {
        int merged = 0;
        for (CartMergeItem mergeItem : items) {
            if (mergeItem == null || mergeItem.getProductId() == null) continue;
            Product product = productRepository.findById(mergeItem.getProductId()).orElse(null);
            if (product == null) continue;
            int qty = mergeItem.getQuantity() != null && mergeItem.getQuantity() > 0
                    ? mergeItem.getQuantity() : 1;
            int available = inventoryService.getAvailableQuantity(product.getId());
            if (qty > available) qty = Math.max(1, available);

            var existing = cartItemRepository.findByUserIdAndProductId(userId, product.getId());
            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(Math.min(item.getQuantity() + qty, available));
                cartItemRepository.save(item);
            } else {
                cartItemRepository.save(CartItem.builder()
                        .userId(userId)
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImage(product.getImageUrl())
                        .quantity(qty)
                        .unitPrice(product.getPrice())
                        .prescriptionRequired(product.getPrescriptionRequired())
                        .build());
            }
            merged++;
        }
        log.info("Merged {} guest cart items for userId={}", merged, userId);
        return getCartSummary(userId);
    }

    @Transactional
    public CartResponse addToCart(Long userId, CartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        var existing = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = request.getQuantity() != null && request.getQuantity() > 0
                    ? request.getQuantity()
                    : item.getQuantity() + 1;
            int available = inventoryService.getAvailableQuantity(product.getId());
            if (newQty > available) {
                throw new IllegalArgumentException("Only " + available
                        + " units of " + product.getName() + " are available");
            }
            item.setQuantity(newQty);
            CartItem saved = cartItemRepository.save(item);
            log.info("Updated cart: userId={}, productId={}, qty={}", userId, request.getProductId(), newQty);
            return CartResponse.fromEntity(saved);
        }

        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .quantity(request.getQuantity() != null && request.getQuantity() > 0
                        ? request.getQuantity() : 1)
                .unitPrice(product.getPrice())
                .prescriptionRequired(product.getPrescriptionRequired())
                .build();

        CartItem saved = cartItemRepository.save(cartItem);
        log.info("Added to cart: userId={}, productId={}, qty={}", userId, product.getId(), saved.getQuantity());
        return CartResponse.fromEntity(saved);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        productRepository.findById(item.getProductId()).ifPresent(product -> {
            int available = inventoryService.getAvailableQuantity(product.getId());
            if (quantity > available) {
                throw new IllegalArgumentException("Only " + available
                        + " units of " + product.getName() + " are available");
            }
        });

        item.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(item);
        log.info("Updated cart quantity: id={}, qty={}", cartItemId, quantity);
        return CartResponse.fromEntity(saved);
    }

    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        cartItemRepository.delete(item);
        log.info("Removed from cart: userId={}, cartItemId={}", userId, cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
        log.info("Cleared cart for userId={}", userId);
    }

    @Transactional(readOnly = true)
    public int getCartCount(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}
