package com.eventhub.event_hub.enums;

public enum UserRole {
    ADMIN,
    USER,
    /** Legacy value kept so rows created by earlier versions still load. Behaves like USER. */
    CLIENT
}
