package com.eventhub.event_hub.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    ADMIN("admin"),
    USER("user"),
    CLIENT("client");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    @JsonValue
    public String getRole() {
        return role;
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        for (UserRole userRole : UserRole.values()) {
            if (userRole.name().equalsIgnoreCase(value) || userRole.role.equalsIgnoreCase(value)) {
                return userRole;
            }
        }
        return null;
    }
}