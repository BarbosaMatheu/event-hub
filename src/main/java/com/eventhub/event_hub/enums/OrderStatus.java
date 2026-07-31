package com.eventhub.event_hub.enums;

public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        if(this == PENDING && (newStatus == COMPLETED || newStatus == CANCELLED)) {
            return true;
        }
        return false;
    }
}
