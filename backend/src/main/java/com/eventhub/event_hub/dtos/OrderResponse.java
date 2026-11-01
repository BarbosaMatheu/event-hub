package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.models.Order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code createdAt} is an absolute instant (so every browser shows it in its own timezone) and
 * {@code expiresInSeconds} is how long a PENDING order still holds its tickets (null otherwise).
 * The server computes the countdown, so clients never need to know the server's timezone or TTL.
 */
public record OrderResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Long expiresInSeconds,
        String customerLogin,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, Clock clock, long pendingTtlMinutes) {
        Instant createdAt = order.getCreatedAt().atZone(clock.getZone()).toInstant();

        Long expiresIn = null;
        if (order.getStatus() == OrderStatus.PENDING) {
            Instant expiresAt = createdAt.plus(Duration.ofMinutes(pendingTtlMinutes));
            expiresIn = Math.max(0L, Duration.between(clock.instant(), expiresAt).getSeconds());
        }

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                createdAt,
                expiresIn,
                order.getUser().getLogin(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
