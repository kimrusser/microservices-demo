package com.demo.kafka;

import com.demo.event.OrderCreatedEvent;
import com.demo.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(@Payload Map<String, Object> message) {
        try {
            log.info("Received OrderCreatedEvent raw message");

            // Manually convert Map to OrderCreatedEvent
            OrderCreatedEvent event = objectMapper.convertValue(message, OrderCreatedEvent.class);

            log.info("Received OrderCreatedEvent for orderId: {}, amount: {}",
                    event.orderId(), event.totalAmount());

            paymentService.processPaymentFromEvent(event);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent", e);
        }
    }
}
