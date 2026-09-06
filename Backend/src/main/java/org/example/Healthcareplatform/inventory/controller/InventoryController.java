package org.example.Healthcareplatform.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.common.CurrentUser;
import org.example.Healthcareplatform.inventory.dto.StockAdjustRequest;
import org.example.Healthcareplatform.inventory.dto.StockInfoResponse;
import org.example.Healthcareplatform.inventory.dto.StockValidateRequest;
import org.example.Healthcareplatform.inventory.dto.StockValidateResponse;
import org.example.Healthcareplatform.inventory.dto.ThresholdRequest;
import org.example.Healthcareplatform.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/stock")
    public ResponseEntity<List<StockInfoResponse>> getAllStock() {
        log.info("GET /api/inventory/stock");
        return ResponseEntity.ok(inventoryService.getAllStockInfo());
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<StockInfoResponse> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<StockInfoResponse>> getLowStock() {
        log.info("GET /api/inventory/low-stock");
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<List<StockInfoResponse.StockHistoryDto>> getHistory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getHistory(productId));
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<StockInfoResponse.StockReservationDto>> getReservations() {
        log.info("GET /api/inventory/reservations");
        return ResponseEntity.ok(inventoryService.getReservations());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<StockInfoResponse.StockHistoryDto>> getRecentActivity() {
        log.info("GET /api/inventory/activity");
        return ResponseEntity.ok(inventoryService.getRecentActivity(20));
    }

    @PostMapping("/validate")
    public ResponseEntity<StockValidateResponse> validateStock(@Valid @RequestBody StockValidateRequest request) {
        log.info("POST /api/inventory/validate — {} items", request.getItems().size());
        List<InventoryService.ReserveItem> items = request.getItems().stream()
                .map(i -> new InventoryService.ReserveItem(i.getProductId(), i.getQuantity()))
                .toList();
        return ResponseEntity.ok(inventoryService.validateStock(items));
    }

    @PostMapping("/{productId}/increment")
    public ResponseEntity<StockInfoResponse> incrementStock(@PathVariable Long productId,
                                                            @Valid @RequestBody StockAdjustRequest request) {
        log.info("POST /api/inventory/{}/increment — qty={}, note={}", productId, request.getQuantity(), request.getNote());
        inventoryService.increaseStock(productId, request.getQuantity(), CurrentUser.userId(), request.getNote());
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @PostMapping("/{productId}/decrement")
    public ResponseEntity<StockInfoResponse> decrementStock(@PathVariable Long productId,
                                                            @Valid @RequestBody StockAdjustRequest request) {
        log.info("POST /api/inventory/{}/decrement — qty={}, note={}", productId, request.getQuantity(), request.getNote());
        inventoryService.decreaseStock(productId, request.getQuantity(), CurrentUser.userId(), request.getNote());
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<StockInfoResponse> adjustStock(@PathVariable Long productId,
                                                         @Valid @RequestBody StockAdjustRequest request) {
        log.info("POST /api/inventory/{}/adjust — qty={}, note={}", productId, request.getQuantity(), request.getNote());
        inventoryService.adjustStock(productId, request.getQuantity(), CurrentUser.userId(), request.getNote());
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @PutMapping("/{productId}/threshold")
    public ResponseEntity<StockInfoResponse> setThreshold(@PathVariable Long productId,
                                                          @Valid @RequestBody ThresholdRequest request) {
        log.info("PUT /api/inventory/{}/threshold — threshold={}", productId, request.getThreshold());
        inventoryService.setLowStockThreshold(productId, request.getThreshold());
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }
}
