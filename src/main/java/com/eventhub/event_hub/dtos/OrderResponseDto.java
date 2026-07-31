package com.eventhub.event_hub.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.eventhub.event_hub.enums.OrderStatus;

public record OrderResponseDto(UUID id,
    OrderStatus status,
    BigDecimal totalAmount,
    LocalDateTime createdAt) {
    public static OrderResponseDto fromEntity(com.eventhub.event_hub.models.Order order) {
        return new OrderResponseDto(
            order.getId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
}