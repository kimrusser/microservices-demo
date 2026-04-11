package com.demo.service;

import com.demo.dto.CreateOrderRequest;
import com.demo.dto.OrderItemRequest;
import com.demo.dto.OrderResponse;
import com.demo.entity.Order;
import com.demo.entity.OrderItem;
import com.demo.entity.OrderStatus;
import com.demo.event.InventoryUpdatedEvent;
import com.demo.event.PaymentProcessedEvent;
import com.demo.kafka.OrderEventProducer;
import com.demo.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventProducer eventProducer;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private OrderService orderService;
    private Order order;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setup() {
        //Setup test data
        order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId("cust-001")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(999.99))
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID().toString())
                .productId("prod-001")
                .productName("Gaming Laptop")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(999.99))
                .subtotal(BigDecimal.valueOf(999.99))
                .build();

        order.setItems(List.of(orderItem));

        OrderItemRequest itemRequest = new OrderItemRequest(
                "prod-001",
                "Gaming Laptop",
                1,
                BigDecimal.valueOf(999.999)
        );

        createOrderRequest = new CreateOrderRequest(
                "cust-001",
                List.of(itemRequest)
        );
    }

    @Test
    void create_Order_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(eventProducer).publishOrderCreated(any());

        // When
        OrderResponse response = orderService.createOrder(createOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo("cust-001");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
        assertThat(response.items()).hasSize(1);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventProducer, times(1)).publishOrderCreated(any());
    }

    @Test
    void getOrderById_Success() {
        // Given
        String orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        OrderResponse response = orderService.getOrder(orderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(orderId);
        assertThat(response.customerId()).isEqualTo("cust-001");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrderById_NotFound() {
        // Given
        String orderId = "non-existent-id";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void handlePaymentResult_PaymentSuccess() {
        // Given
        String orderId = order.getId();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                orderId,
                "payment-id",
                true,
                "Payment successful",
                LocalDateTime.now()
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.handlePaymentResult(event);

        // Then
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void handlePaymentResult_PaymentFailure() {
        // Given
        String orderId = order.getId();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                orderId,
                "payment-id",
                false,
                "Payment failed",
                LocalDateTime.now()
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.handlePaymentResult(event);

        // Then
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void handleInventoryResult_InventorySuccess() {
        // Given
        String orderId = order.getId();
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);

        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                orderId,
                true,
                "Inventory confirmed",
                LocalDateTime.now()
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.handleInventoryResult(event);

        // Then
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void cancelOrder_Success() {
        // Given
        String orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderResponse response = orderService.cancelOrder(orderId);

        // Then
        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrdersByCustomer_Success() {
        // Given
        String customerId = "cust-001";
        when(orderRepository.findByCustomerId(customerId))
                .thenReturn(List.of(order));

        // When
        List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo(customerId);

        verify(orderRepository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void getAllOrders_Success() {
        // Given
        when(orderRepository.findAll()).thenReturn(List.of(order));

        // When
        List<OrderResponse> responses = orderService.getAllOrders();

        // Then
        assertThat(responses).hasSize(1);
        verify(orderRepository, times(1)).findAll();
    }
}