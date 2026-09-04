package com.eventhub.event_hub.models;

import com.eventhub.event_hub.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void pendingCanTransitionToCompleted() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void pendingCanTransitionToCancelled() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void completedCannotTransitionToCancelled() {
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void cancelledCannotTransitionToCompleted() {
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void pendingCannotTransitionToItself() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING));
    }
}
