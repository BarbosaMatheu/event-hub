package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String login,
        @NotBlank @Size(max = 128) String password
) {}
