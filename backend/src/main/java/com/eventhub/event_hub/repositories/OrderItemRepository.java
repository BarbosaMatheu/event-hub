package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    boolean existsByEventId(UUID eventId);
}
