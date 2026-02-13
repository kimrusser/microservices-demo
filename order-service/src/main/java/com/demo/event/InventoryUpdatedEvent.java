package com.demo.event;

import java.time.LocalDateTime;

public record InventoryUpdatedEvent (
    String orderId,
    boolean success,
    String message,
    LocalDateTime updatedAt
) {}
