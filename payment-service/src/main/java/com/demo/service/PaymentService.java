package com.demo.service;

import com.demo.dto.PaymentResponse;
import com.demo.dto.ProcessPaymentRequest;
import com.demo.entity.Payment;
import com.demo.entity.PaymentStatus;
import com.demo.event.OrderCreatedEvent;
import com.demo.event.PaymentProcessedEvent;
import com.demo.kafka.PaymentEventProducer;
import com.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    /**
     * Manually process payment (called via REST API).
     */
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for orderId: {}", request.orderId());

        if (paymentRepository.existsByOrderId(request.orderId())) {
            throw new IllegalStateException("Payment already exists for order: " + request.orderId());
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .customerId(request.customerId())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PROCESSING)
                .build();

        Payment saved = paymentRepository.save(payment);

        // Simulate payment gateway processing
        boolean paymentSuccess = simulatePaymentGateway(request.amount());

        if (paymentSuccess) {
            saved.setStatus(PaymentStatus.COMPLETED);
            saved.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            saved.setProcessedAt(LocalDateTime.now());
            log.info("Payment completed for orderId: {}", request.orderId());
        } else {
            saved.setStatus(PaymentStatus.FAILED);
            saved.setFailureReason("Simulated gateway decline");
            log.warn("Payment failed for orderId: {}", request.orderId());
        }

        Payment result = paymentRepository.save(saved);

        // Publish event to Order Service
        publishPaymentEvent(result);

        return mapToResponse(result);
    }

    /**
     * Auto-process payment triggered by Kafka order-created event.
     */
    public void processPaymentFromEvent(OrderCreatedEvent event) {
        log.info("Auto-processing payment from Kafka event for orderId: {}", event.orderId());

        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("Payment already exists for orderId: {}, skipping", event.orderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .paymentMethod("AUTO")
                .status(PaymentStatus.PROCESSING)
                .build();

        Payment saved = paymentRepository.save(payment);

        boolean success = simulatePaymentGateway(event.totalAmount());

        if (success) {
            saved.setStatus(PaymentStatus.COMPLETED);
            saved.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            saved.setProcessedAt(LocalDateTime.now());
        } else {
            saved.setStatus(PaymentStatus.FAILED);
            saved.setFailureReason("Insufficient funds");
        }

        Payment result = paymentRepository.save(saved);
        publishPaymentEvent(result);
    }

    public PaymentResponse getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NoSuchElementException("Payment not found: " + paymentId));
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NoSuchElementException("No payment found for order: " + orderId));
    }

    public List<PaymentResponse> getPaymentsByCustomer(String customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Simulates a payment gateway. Fails if amount > 10000 to demonstrate failure handling.
     */
    private boolean simulatePaymentGateway(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("10000")) <= 0;
    }

    private void publishPaymentEvent(Payment payment) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                payment.getOrderId(),
                payment.getId(),
                payment.getStatus() == PaymentStatus.COMPLETED,
                payment.getStatus() == PaymentStatus.COMPLETED
                        ? "Payment processed successfully"
                        : payment.getFailureReason(),
                LocalDateTime.now()
        );

        eventProducer.publishPaymentProcessed(event);
    }

    private PaymentResponse mapToResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getStatus().name(),
                p.getPaymentMethod(),
                p.getTransactionId(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getProcessedAt()
        );
    }
}
