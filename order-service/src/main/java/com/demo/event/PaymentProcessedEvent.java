package com.demo.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessedEvent {

    private String orderId;
    private String paymentId;
    private boolean success;
    private String message;
    private LocalDateTime processedAt;
}
