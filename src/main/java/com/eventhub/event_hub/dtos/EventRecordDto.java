package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventRecordDto(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String local,
    @NotNull LocalDateTime dateTime,
    @NotNull Integer maximumCapacity,
    @NotNull BigDecimal price,
    @NotNull UUID categoryId
) {}