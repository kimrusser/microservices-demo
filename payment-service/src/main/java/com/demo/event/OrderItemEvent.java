package com.demo.event;

import java.math.BigDecimal;

public record OrderItemEvent(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
