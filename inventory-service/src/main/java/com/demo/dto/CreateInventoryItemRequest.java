package com.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryItemRequest(
        @NotBlank(message = "Product ID is required")
        String productId,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotNull @Min(value = 0, message = "Quantity cannot be negative")
        Integer initialQuantity
) {}
