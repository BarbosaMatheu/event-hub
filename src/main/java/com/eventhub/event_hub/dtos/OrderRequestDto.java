package com.eventhub.event_hub.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequestDto(
    @NotEmpty(message = "The items list must not be empty")
    @Valid 
    List<OrderItemRequestDto> items
) {}