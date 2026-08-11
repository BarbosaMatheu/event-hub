package com.eventhub.event_hub.dtos;

import com.eventhub.event_hub.enums.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDTO(
    @NotBlank String login,
    @NotBlank String password,
    @NotNull UserRole role
) {}
