package com.eventhub.event_hub.models;

import com.eventhub.event_hub.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void pendingCanBeCompletedOrCancelled() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void finalStatesCannotChange() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.COMPLETED.canTransitionTo(target));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
        }
    }

    @Test
    void pendingCannotTransitionToItself() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING));
    }
}
