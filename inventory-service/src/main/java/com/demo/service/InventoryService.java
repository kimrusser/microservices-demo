package com.demo.service;

import com.demo.dto.CreateInventoryItemRequest;
import com.demo.dto.InventoryItemResponse;
import com.demo.dto.ReserveStockRequest;
import com.demo.entity.InventoryItem;
import com.demo.entity.Reservation;
import com.demo.entity.ReservationStatus;
import com.demo.event.InventoryUpdatedEvent;
import com.demo.kafka.InventoryEventProducer;
import com.demo.repository.InventoryItemRepository;
import com.demo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryEventProducer eventProducer;

    /**
     * Create a new inventory item with initial stock.
     */
    public InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request) {
        log.info("Creating inventory item for productId: {}", request.productId());

        if (inventoryItemRepository.existsByProductId(request.productId())) {
            throw new IllegalStateException("Inventory item already exists for product: " + request.productId());
        }

        InventoryItem item = InventoryItem.builder()
                .productId(request.productId())
                .productName(request.productName())
                .availableQuantity(request.initialQuantity())
                .reservedQuantity(0)
                .build();

        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Inventory item created: {} with quantity: {}", saved.getProductId(), saved.getAvailableQuantity());

        return mapToResponse(saved);
    }

    /**
     * Manually reserve stock (called via REST API).
     */
    public InventoryItemResponse reserveStock(ReserveStockRequest request) {
        log.info("Reserving stock for productId: {}, quantity: {}, orderId: {}",
                request.productId(), request.quantity(), request.orderId());

        if (reservationRepository.existsByOrderId(request.orderId())) {
            throw new IllegalStateException("Reservation already exists for order: " + request.orderId());
        }

        // Use pessimistic locking to prevent race conditions
        InventoryItem item = inventoryItemRepository.findByProductIdWithLock(request.productId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + request.productId()));

        // Reserve stock (throws exception if insufficient)
        item.reserveStock(request.quantity());

        // Create reservation record
        Reservation reservation = Reservation.builder()
                .orderId(request.orderId())
                .productId(request.productId())
                .quantity(request.quantity())
                .status(ReservationStatus.RESERVED)
                .build();

        inventoryItemRepository.save(item);
        reservationRepository.save(reservation);

        log.info("Stock reserved successfully for orderId: {}", request.orderId());

        return mapToResponse(item);
    }

    /**
     * Confirm reservation (convert reserved → sold).
     * Called when payment succeeds.
     */
    public void confirmReservationForOrder(String orderId) {
        log.info("Confirming reservation for orderId: {}", orderId);

        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No reservation found for order: " + orderId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Reservation already processed for orderId: {}, status: {}", orderId, reservation.getStatus());
            return;
        }

        InventoryItem item = inventoryItemRepository.findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + reservation.getProductId()));

        // Confirm reservation (remove from reserved count)
        item.confirmReservation(reservation.getQuantity());

        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());

        inventoryItemRepository.save(item);
        reservationRepository.save(reservation);

        log.info("Reservation confirmed for orderId: {}", orderId);

        // Publish success event to Order Service
        publishInventoryEvent(orderId, true, "Inventory confirmed successfully");
    }

    /**
     * Release reservation (convert reserved → available).
     * Called when payment fails.
     */
    public void releaseReservationForOrder(String orderId) {
        log.info("Releasing reservation for orderId: {}", orderId);

        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No reservation found for order: " + orderId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Reservation already processed for orderId: {}, status: {}", orderId, reservation.getStatus());
            return;
        }

        InventoryItem item = inventoryItemRepository.findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + reservation.getProductId()));

        // Release reserved stock back to available
        item.releaseReservedStock(reservation.getQuantity());

        // Update reservation status
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());

        inventoryItemRepository.save(item);
        reservationRepository.save(reservation);

        log.info("Reservation released for orderId: {}", orderId);

        // Publish failure event to Order Service
        publishInventoryEvent(orderId, false, "Inventory reservation released due to payment failure");
    }

    /**
     * Add stock to existing inventory item.
     */
    public InventoryItemResponse addStock(String productId, Integer quantity) {
        log.info("Adding {} units to productId: {}", quantity, productId);

        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);

        InventoryItem updated = inventoryItemRepository.save(item);
        log.info("Stock added. New available quantity: {}", updated.getAvailableQuantity());

        return mapToResponse(updated);
    }

    public InventoryItemResponse getInventoryItem(String productId) {
        return inventoryItemRepository.findByProductId(productId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
    }

    public List<InventoryItemResponse> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ─── Private helpers ─────────────────────────────────────────────────────

    private void publishInventoryEvent(String orderId, boolean success, String message) {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                orderId,
                success,
                message,
                LocalDateTime.now()
        );

        eventProducer.publishInventoryUpdated(event);
    }

    private InventoryItemResponse mapToResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
