package com.eventhub.event_hub.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "The items list must not be empty")
        @Size(max = 10, message = "An order can contain at most 10 different events")
        @Valid
        List<OrderItemRequest> items
) {}
