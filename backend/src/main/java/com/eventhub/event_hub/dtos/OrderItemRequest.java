package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The client only says WHAT and HOW MANY: the price always comes from the server. */
public record OrderItemRequest(
        @NotNull UUID eventId,
        @NotNull @Min(1) @Max(10) Integer quantity
) {}
