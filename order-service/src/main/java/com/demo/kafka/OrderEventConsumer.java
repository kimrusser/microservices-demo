package com.demo.kafka;

import com.demo.event.InventoryUpdatedEvent;
import com.demo.event.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for orderId: {}, success: {}",
                event.getOrderId(), event.isSuccess());
        orderService.handlePaymentResult(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Received InventoryUpdatedEvent for orderId: {}, success: {}",
                event.getOrderId(), event.isSuccess());
        orderService.handleInventoryResult(event);
    }
}
