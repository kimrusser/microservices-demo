package com.demo.controller;

import com.demo.dto.PaymentResponse;
import com.demo.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private PaymentService paymentService;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        paymentResponse = new PaymentResponse(
                "payment-id",
                "order-id",
                "cust-001",
                BigDecimal.valueOf(999.99),
                "COMPLETED",
                "AUTO",
                "TXN-12345",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void getPaymentByOrderId_Success() throws Exception {
        // Given
        String orderId = "order-id";
        when(paymentService.getPaymentByOrderId(orderId))
                .thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(get("/api/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.transactionId").value("TXN-12345"));
    }

    @Test
    void getPaymentById_Success() throws Exception {
        // Given
        String paymentId = "payment-id";
        when(paymentService.getPayment(paymentId))
                .thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.customerId").value("cust-001"));
    }

    @Test
    void getPaymentsByCustomer_Success() throws Exception {
        // Given
        String customerId = "cust-001";
        when(paymentService.getPaymentsByCustomer(customerId))
                .thenReturn(List.of(paymentResponse));

        // When & Then
        mockMvc.perform(get("/api/payments/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(customerId));
    }

    @Test
    void getAllPayments_Success() throws Exception {
        // Given
        when(paymentService.getAllPayments())
                .thenReturn(List.of(paymentResponse));

        // When & Then
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

}
