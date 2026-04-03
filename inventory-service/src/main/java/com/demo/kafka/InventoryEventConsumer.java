package com.demo.kafka;

import com.demo.event.PaymentProcessedEvent;
import com.demo.service.InventoryService;
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
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePaymentProcessed(@Payload Map<String, Object> message) {
        try {
            PaymentProcessedEvent event = objectMapper.convertValue(message, PaymentProcessedEvent.class);

            log.info("Received PaymentProcessedEvent for orderId: {}, success: {}",
                    event.orderId(), event.success());

            if (event.success()) {
                inventoryService.confirmReservationForOrder(event.orderId());
            } else {
                inventoryService.releaseReservationForOrder(event.orderId());
            }
        } catch (Exception e) {
            log.error("Error processing PaymentProcessedEvent", e);
        }
    }
}
