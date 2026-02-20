package com.demo.service;

import com.demo.dto.CreateOderRequest;
import com.demo.dto.OrderItemRequest;
import com.demo.dto.OrderItemResponse;
import com.demo.dto.OrderResponse;
import com.demo.entity.Order;
import com.demo.entity.OrderItem;
import com.demo.entity.OrderStatus;
import com.demo.event.InventoryUpdatedEvent;
import com.demo.event.OrderCreatedEvent;
import com.demo.event.OrderItemEvent;
import com.demo.event.PaymentProcessedEvent;
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
        log.info("Creating order for customer: {}", request.customerId());

        //Build order items
        // Java 21: Use method reference with records
        List<OrderItem> items = request.items().stream()
                .map(this::mapToOderItem)
                .toList(); // Java 16+: toList() instead of collect(Collectors.toList())


        log.info("Request received: customerId={}, items count={}",
                request.customerId(), request.items().size());
        //Calculate total
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO,(BigDecimal::add));

        log.info("Calculated total: {}", total);

        Order order = Order.builder()
                .customerId(request.customerId())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();

        log.info("Built order: customerId={}, totalAmount={}, status={}",
                order.getCustomerId(), order.getTotalAmount(), order.getStatus());

        //Link items to order
        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        log.info("After setting items, totalAmount={}", order.getTotalAmount());
        // var: Obviously returns Order (from save method signature)
        var saved = orderRepository.save(order);
        log.info("Order created with ID: {}, total: {}", saved.getId(), saved.getTotalAmount());

        // Publish Kafka event
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getTotalAmount(),
                items.stream().map(this::mapToItemEvent).toList(),
                LocalDateTime.now()
        );

        eventProducer.publishOrderCreated(event);

        return mapToResponse(saved);
    }

    public OrderResponse getOrder(String orderId) {
       return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse cancelOrder(String orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // Java 21: Pattern matching for switch
        var canCancel = switch (order.getStatus()) {
            case COMPLETED -> false;
            case CANCELLED -> throw new IllegalStateException("Order already cancelled");
            default -> true;
        };

        if (!canCancel) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        var saved = orderRepository.save(order);

        eventProducer.publishOrderCancelled(orderId, "Customer requested cancellation");
        return mapToResponse(saved);
    }

    public void handlePaymentResult(PaymentProcessedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            // Simple ternary operator (stable feature)
            OrderStatus newStatus = event.success()
                    ? OrderStatus.PAYMENT_COMPLETED
                    : OrderStatus.PAYMENT_FAILED;

            if (event.success()) {
                log.info("Payment completed for order: {}", event.orderId());
            } else {
                log.warn("Payment failed for order: {}. Reason: {}",
                        event.orderId(), event.message());
            }

            order.setStatus(newStatus);
            orderRepository.save(order);
        });
    }

    public void handleInventoryResult(InventoryUpdatedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            OrderStatus newStatus = event.success()
                    ? OrderStatus.COMPLETED
                    : OrderStatus.INVENTORY_FAILED;

            String logMessage = event.success()
                    ? "Inventory reserved, order completed: " + event.orderId()
                    : "Inventory update failed for order: " + event.orderId();

            log.info(logMessage);
            order.setStatus(newStatus);
            orderRepository.save(order);
        });
    }



    private OrderItem mapToOderItem(OrderItemRequest req) {
        return OrderItem.builder()
                .productId(req.productId())
                .productName(req.productName())
                .quantity(req.quantity())
                .unitPrice(req.unitPrice())
                .subtotal(req.unitPrice().multiply(BigDecimal.valueOf(req.quantity())))
                .build();
    }

    private OrderItemEvent mapToItemEvent(OrderItem item) {
        return new OrderItemEvent(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
