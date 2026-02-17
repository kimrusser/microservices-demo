package com.demo.controller;

import com.demo.dto.CreateOderRequest;
import com.demo.dto.OrderResponse;
import com.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOderRequest request) {
        log.info("POST /api/orders - Creating order for customer: {}", request.customerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("GET /api/orders{}", orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("GET /api/orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerId) {
        log.info("GET /api/orders/customer/{}", customerId);
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        log.info("PATCH /api/orders/{}/cancel", orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

}
