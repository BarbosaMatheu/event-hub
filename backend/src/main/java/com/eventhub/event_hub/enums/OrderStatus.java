package com.eventhub.event_hub.enums;

public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    /** A pending order can be completed (paid) or cancelled; completed/cancelled are final. */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return this == PENDING && (newStatus == COMPLETED || newStatus == CANCELLED);
    }
}
