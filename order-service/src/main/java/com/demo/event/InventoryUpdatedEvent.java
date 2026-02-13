package com.demo.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUpdatedEvent {

    private String orderId;
    private boolean success;
    private String message;
    private LocalDateTime updatedAt;
}
