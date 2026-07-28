package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotBlank;

public class CategoryRecordDto {
    @NotBlank String name;
}
