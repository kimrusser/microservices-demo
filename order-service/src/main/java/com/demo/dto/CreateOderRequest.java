package com.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/*
* Record automatically generates
* Constructor: new CreateOrderRequest(String customerId,...)
* Getters: createOrderRequest.getCustomerId, createOrderRequest.getItems
* equals() and hashCode()
* toString()
* Immutable - Cannot change values after creation
* */
public record CreateOderRequest (
    @NotBlank(message = "Customer ID is required")
    String customerId,

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    List<OrderItemRequest> items
) {}
