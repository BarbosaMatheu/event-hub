package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.models.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        int capacity,
        int ticketsSold,
        int availableTickets,
        BigDecimal price,
        String imageUrl,
        CategoryResponse category,
        LocalDateTime createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartsAt(),
                event.getCapacity(),
                event.getTicketsSold(),
                event.getAvailableTickets(),
                event.getPrice(),
                event.getImageUrl(),
                CategoryResponse.from(event.getCategory()),
                event.getCreatedAt());
    }
}
