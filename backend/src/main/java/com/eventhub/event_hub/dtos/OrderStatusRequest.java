package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {}
