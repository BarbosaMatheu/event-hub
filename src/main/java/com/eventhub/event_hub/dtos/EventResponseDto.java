package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.models.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponseDto(
    UUID id,
    String title,
    String description,
    String local,
    LocalDateTime dateTime,
    Integer maximumCapacity,
    BigDecimal price,
    UUID categoryId,
    String categoryName
) {

    public static EventResponseDto fromEntity(Event event) {
        return new EventResponseDto(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getLocal(),
            event.getDateTime(),
            event.getMaximumCapacity(),
            event.getPrice(),
            event.getCategory() != null ? event.getCategory().getId() : null,
            event.getCategory() != null ? event.getCategory().getName() : null
        );
    }
}
