package com.demo.dto;

import java.time.LocalDateTime;

public record InventoryItemResponse(
        String id,
        String productId,
        String productName,
        Integer availableQuantity,
        Integer reservedQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
