package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequestDto(
    @NotNull @Positive BigDecimal price,
    @NotNull @Positive Integer quantity
)  {}