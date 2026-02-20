package com.demo.kafka;

import com.demo.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId: {}", event.orderId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderCreatedTopic, event.orderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderCreatedEvent for orderId: {}",
                        event.orderId(), ex);
            } else {
                log.info("OrderCreatedEvent published successfully. Offset: {}",
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void publishOrderCancelled(String orderId, String reason) {
        log.info("Publishing OrderCancelledEvent for orderId: {}", orderId);

        var event = java.util.Map.of(
                "orderId", orderId,
                "reason", reason
        );

        kafkaTemplate.send(orderCancelledTopic, orderId, event);
    }
}
