package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.models.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
