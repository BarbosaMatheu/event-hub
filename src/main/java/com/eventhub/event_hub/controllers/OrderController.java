package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.OrderRequestDto;
import com.eventhub.event_hub.dtos.OrderResponseDto;
import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid OrderRequestDto dto) {
        OrderResponseDto response = orderService.createOrder(dto.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'USER')")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID id) {
        OrderResponseDto response = orderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateStatus(
        @PathVariable UUID id, 
        @RequestBody OrderStatus status) {
        
        OrderResponseDto orderResponseDto = orderService.updateStatus(id, status);
        return ResponseEntity.ok(orderResponseDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> deleteOrder(@PathVariable UUID id) {
        OrderResponseDto response = orderService.deleteOrder(id);
        return ResponseEntity.ok(response);
    }
}