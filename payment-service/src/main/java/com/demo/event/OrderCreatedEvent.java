package com.demo.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {}
