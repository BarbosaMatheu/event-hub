package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public registration. There is intentionally no role field: self-registered accounts are
 * always regular users, so nobody can create an admin through the public API.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
        String login,

        // 72 bytes is BCrypt's hard input limit.
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {}
