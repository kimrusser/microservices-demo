package com.demo.controller;

import com.demo.dto.CreateOrderRequest;
import com.demo.dto.OrderItemRequest;
import com.demo.dto.OrderResponse;
import com.demo.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderItemRequest itemRequest;
    private CreateOrderRequest createOrderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        itemRequest = new OrderItemRequest(
                "prod-001",
                "Gaming Laptop",
                1,
                BigDecimal.valueOf(999.99)
        );

        createOrderRequest = new CreateOrderRequest(
                "cust-001",
                List.of(itemRequest)
        );

        orderResponse = new OrderResponse(
                "order-id",
                "cust-001",
                "PENDING",
                BigDecimal.valueOf(999.99),
                List.of(),
                LocalDateTime.now(),
                null
        );
    }

    @Test
    void createOrder_Success() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(999.99));
    }

    @Test
    void createOrder_ValidationError_EmptyCustomerId() throws Exception {
        // Given - Override only the invalid field
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",  // Invalid empty customer ID
                List.of(itemRequest)
        );

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_Success() throws Exception {
        // Given - Create completed order response
        String orderId = "order-id";
        OrderResponse completedResponse = new OrderResponse(
                orderId,
                "cust-001",
                "COMPLETED",
                BigDecimal.valueOf(999.99),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrder(orderId)).thenReturn(completedResponse);

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void cancelOrder_Success() throws Exception {
        // Given
        String orderId = "order-id";
        OrderResponse cancelledResponse = new OrderResponse(
                orderId,
                "cust-001",
                "CANCELLED",
                BigDecimal.valueOf(999.99),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.cancelOrder(orderId)).thenReturn(cancelledResponse);

        // When & Then
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
