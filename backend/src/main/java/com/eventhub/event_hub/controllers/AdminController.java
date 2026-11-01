package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.AdminStatsResponse;
import com.eventhub.event_hub.dtos.OrderResponse;
import com.eventhub.event_hub.dtos.PageResponse;
import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.services.AdminStatsService;
import com.eventhub.event_hub.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Back-office endpoints (ADMIN only)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminStatsService statsService;
    private final OrderService orderService;

    public AdminController(AdminStatsService statsService, OrderService orderService) {
        this.statsService = statsService;
        this.orderService = orderService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard numbers: events, orders, tickets sold, revenue, top events")
    public AdminStatsResponse stats() {
        return statsService.getStats();
    }

    @GetMapping("/orders")
    @Operation(summary = "All orders, optionally filtered by status")
    public PageResponse<OrderResponse> orders(@RequestParam(required = false) OrderStatus status,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return orderService.findAll(status, pageable);
    }
}
