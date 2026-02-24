package com.demo.kafka;

import com.demo.event.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing PaymentProcessedEvent for orderId: {}, success: {}",
                event.orderId(), event.success());

        kafkaTemplate.send(paymentProcessedTopic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentProcessedEvent", ex);
                    } else {
                        log.info("PaymentProcessedEvent published. Offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
