package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotBlank;

public record CategoryRecordDto(
    @NotBlank String name
) {}