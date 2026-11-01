package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.models.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderItemResponse(
        UUID eventId,
        String eventTitle,
        String eventLocation,
        LocalDateTime eventStartsAt,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getEvent().getId(),
                item.getEvent().getTitle(),
                item.getEvent().getLocation(),
                item.getEvent().getStartsAt(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }
}
