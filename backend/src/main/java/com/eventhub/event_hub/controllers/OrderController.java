package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.OrderRequest;
import com.eventhub.event_hub.dtos.OrderResponse;
import com.eventhub.event_hub.dtos.OrderStatusRequest;
import com.eventhub.event_hub.dtos.PageResponse;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Ticket purchases. Users only ever see their own orders.")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Buy tickets",
            description = "Reserves the tickets immediately (order starts as PENDING). "
                    + "409 if there are not enough tickets left; unpaid orders expire automatically.")
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal User user,
                                                @RequestBody @Valid OrderRequest request) {
        OrderResponse created = orderService.create(user, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "My orders (newest first)")
    public PageResponse<OrderResponse> mine(@AuthenticationPrincipal User user,
                                            @PageableDefault(size = 10) Pageable pageable) {
        return orderService.findMine(user, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return orderService.findById(user, id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay a pending order (simulated payment)")
    public OrderResponse pay(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return orderService.pay(user, id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending order and return its tickets")
    public OrderResponse cancel(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return orderService.cancel(user, id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: change an order's status")
    public OrderResponse updateStatus(@PathVariable UUID id, @RequestBody @Valid OrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
