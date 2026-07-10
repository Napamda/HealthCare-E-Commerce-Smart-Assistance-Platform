package org.example.Healthcareplatform.cart.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.cart.dto.CartRequest;
import org.example.Healthcareplatform.cart.dto.CartResponse;
import org.example.Healthcareplatform.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication auth) {
        Long userId = getUserId(auth);
        log.info("GET /api/cart — userId={}", userId);
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartCount(Authentication auth) {
        Long userId = getUserId(auth);
        int count = cartService.getCartCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestBody CartRequest request,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("POST /api/cart/add — userId={}, productId={}", userId, request.getProductId());
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam int quantity,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("PUT /api/cart/{} — qty={}", cartItemId, quantity);
        CartResponse response = cartService.updateQuantity(userId, cartItemId, quantity);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long cartItemId,
            Authentication auth) {
        Long userId = getUserId(auth);
        log.info("DELETE /api/cart/{} — userId={}", cartItemId, userId);
        cartService.removeFromCart(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication auth) {
        Long userId = getUserId(auth);
        log.info("DELETE /api/cart/clear — userId={}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
