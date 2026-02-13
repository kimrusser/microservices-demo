package com.demo.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String id,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
