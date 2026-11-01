package com.eventhub.event_hub.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminStatsResponse(
        long totalEvents,
        long upcomingEvents,
        long totalCategories,
        long totalUsers,
        long pendingOrders,
        long completedOrders,
        long cancelledOrders,
        long ticketsSold,
        BigDecimal revenue,
        List<TopEvent> topEvents
) {
    public record TopEvent(UUID id, String title, int ticketsSold, int capacity, BigDecimal price) {}
}
