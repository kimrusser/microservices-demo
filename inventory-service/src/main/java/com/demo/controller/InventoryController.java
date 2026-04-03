package com.demo.controller;

import com.demo.dto.CreateInventoryItemRequest;
import com.demo.dto.InventoryItemResponse;
import com.demo.dto.ReserveStockRequest;
import com.demo.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/items")
    public ResponseEntity<InventoryItemResponse> createInventoryItem(
            @Valid @RequestBody CreateInventoryItemRequest request) {
        log.info("POST /api/inventory/items for productId: {}", request.productId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.createInventoryItem(request));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryItemResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        log.info("POST /api/inventory/reserve for productId: {}, orderId: {}",
                request.productId(), request.orderId());
        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }

    @PatchMapping("/items/{productId}/add-stock")
    public ResponseEntity<InventoryItemResponse> addStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        log.info("PATCH /api/inventory/items/{}/add-stock, quantity: {}", productId, quantity);
        return ResponseEntity.ok(inventoryService.addStock(productId, quantity));
    }

    @GetMapping("/items/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventoryItem(productId));
    }

    @GetMapping("/items")
    public ResponseEntity<List<InventoryItemResponse>> getAllInventoryItems() {
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
