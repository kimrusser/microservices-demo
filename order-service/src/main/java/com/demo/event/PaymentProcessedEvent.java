package com.demo.event;

import java.time.LocalDateTime;

public record PaymentProcessedEvent (
    String orderId,
    String paymentId,
    boolean success,
    String message,
    LocalDateTime processedAt
) {}
