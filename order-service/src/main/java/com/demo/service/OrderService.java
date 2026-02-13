package com.demo.service;

import com.demo.dto.CreateOderRequest;
import com.demo.dto.OrderItemRequest;
import com.demo.dto.OrderItemResponse;
import com.demo.dto.OrderResponse;
import com.demo.entity.Order;
import com.demo.entity.OrderItem;
import com.demo.entity.OrderStatus;
import com.demo.event.OrderCreatedEvent;
import com.demo.event.OrderItemEvent;
import com.demo.kafka.OrderEventProducer;
import com.demo.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;


    public OrderResponse createOrder(CreateOderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        //Build order items
        List<OrderItem> items = request.getItems().stream()
                .map(this::mapToOderItem)
                .toList();

        //Calculate total
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO,(BigDecimal::add));

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .build();

        //Link items to order
        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getId());

        // Publish Kafka event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .items(items.stream().map(this::mapToItemEvent).collect(Collectors.toList()))
                .build();

        eventProducer.publishOrderCreated(event);

        return mapToResponse(saved);
    }

    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        return mapToResponse(order);
    }



    private OrderItem mapToOderItem(OrderItemRequest req) {
        return OrderItem.builder()
                .productId(req.getProductId())
                .productName(req.getProductName())
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .subtotal(req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
                .build();
    }

    private OrderItemEvent mapToItemEvent(OrderItem item) {
        return OrderItemEvent.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
