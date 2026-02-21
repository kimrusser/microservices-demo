package com.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        String customerId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String transactionId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {}
