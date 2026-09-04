package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.models.Category;

import java.util.UUID;

public record CategoryResponseDto(UUID id, String name) {

    public static CategoryResponseDto fromEntity(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }
}
