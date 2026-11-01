package com.eventhub.event_hub.dtos;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {}
