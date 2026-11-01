package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.models.User;

public record UserResponse(String id, String login, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getRole());
    }
}
