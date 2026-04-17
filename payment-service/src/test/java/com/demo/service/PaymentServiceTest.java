package com.demo.service;

import com.demo.dto.PaymentResponse;
import com.demo.entity.Payment;
import com.demo.entity.PaymentStatus;
import com.demo.event.OrderCreatedEvent;
import com.demo.event.OrderItemEvent;
import com.demo.kafka.PaymentEventProducer;
import com.demo.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventProducer eventProducer;
    @InjectMocks
    private PaymentService paymentService;
    private Payment payment;
    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId("order-id")
                .customerId("cust-001")
                .amount(BigDecimal.valueOf(999.99))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod("AUTO")
                .transactionId("TXN-12345")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        OrderItemEvent itemEvent = new OrderItemEvent(
                "prod-001",
                "Gaming Laptop",
                1,
                BigDecimal.valueOf(999.99)
        );

        orderCreatedEvent = new OrderCreatedEvent(
                "order-id",
                "cust-001",
                BigDecimal.valueOf(999.99),
                List.of(itemEvent),
                LocalDateTime.now()
        );
    }

    @Test
    void processPaymentFromEvent_Success_AmountUnder10000() {
        // Given
        when(paymentRepository.existsByOrderId(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        doNothing().when(eventProducer).publishPaymentProcessed(any());

        // When
        paymentService.processPaymentFromEvent(orderCreatedEvent);

        // Then
        verify(paymentRepository, times(1)).existsByOrderId(any());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(eventProducer, times(1)).publishPaymentProcessed(any());
    }

    @Test
    void processPaymentFromEvent_Failure_AmountOver10000() {
        // Given
        OrderItemEvent itemEvent = new OrderItemEvent(
                "prod-001",
                "Gaming Laptop",
                15,
                BigDecimal.valueOf(999.99)
        );

        OrderCreatedEvent largeOrderEvent = new OrderCreatedEvent(
                "order-id",
                "cust-001",
                BigDecimal.valueOf(14999.85),
                List.of(itemEvent),
                LocalDateTime.now()
        );

        when(paymentRepository.existsByOrderId(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        doNothing().when(eventProducer).publishPaymentProcessed(any());

        // When
        paymentService.processPaymentFromEvent(largeOrderEvent);

        // Then
        verify(paymentRepository, times(1)).existsByOrderId(any());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(eventProducer, times(1)).publishPaymentProcessed(any());
    }

    @Test
    void processPaymentFromEvent_DuplicateEvent_Skipped() {
        // Given - Payment already exists (idempotency check)
        when(paymentRepository.existsByOrderId("order-id")).thenReturn(true);

        // When
        paymentService.processPaymentFromEvent(orderCreatedEvent);

        // Then - Should skip processing
        verify(paymentRepository, times(1)).existsByOrderId("order-id");
        verify(paymentRepository, never()).save(any());
        verify(eventProducer, never()).publishPaymentProcessed(any());
    }

    @Test
    void getPaymentByOrderId_Success() {
        // Given
        String orderId = "order-id";
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
        assertThat(response.transactionId()).isEqualTo("TXN-12345");

        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentByOrderId_NotFound() {
        // Given
        String orderId = "non-existent-order";
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No payment found for order: ");

        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentById_Success() {
        // Given
        String paymentId = payment.getId();
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPayment(paymentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.customerId()).isEqualTo("cust-001");

        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    void getPaymentsByCustomer_Success() {
        // Given
        String customerId = "cust-001";
        when(paymentRepository.findByCustomerId(customerId))
                .thenReturn(List.of(payment));

        // When
        List<PaymentResponse> responses = paymentService.getPaymentsByCustomer(customerId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo(customerId);

        verify(paymentRepository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void getAllPayments_Success() {
        // Given
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        // When
        List<PaymentResponse> responses = paymentService.getAllPayments();

        // Then
        assertThat(responses).hasSize(1);
        verify(paymentRepository, times(1)).findAll();
    }
}
