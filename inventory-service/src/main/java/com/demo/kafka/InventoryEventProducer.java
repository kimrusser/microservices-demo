package com.demo.kafka;

import com.demo.event.InventoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-updated}")
    private String inventoryUpdatedTopic;

    public void publishInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Publishing InventoryUpdatedEvent for orderId: {}, success: {}",
                event.orderId(), event.success());

        kafkaTemplate.send(inventoryUpdatedTopic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryUpdatedEvent", ex);
                    } else {
                        log.info("InventoryUpdatedEvent published. Offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
