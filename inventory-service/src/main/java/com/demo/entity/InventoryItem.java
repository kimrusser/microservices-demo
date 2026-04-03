package com.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean hasAvailableStock(Integer quantity) {
        return availableQuantity >= quantity;
    }

    public void reserveStock(Integer quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException("Insufficient stock for product: " + productName);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void releaseReservedStock(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void confirmReservation(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved");
        }
        reservedQuantity -= quantity;
    }
}
