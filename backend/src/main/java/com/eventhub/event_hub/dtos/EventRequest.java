package com.eventhub.event_hub.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 200) String location,
        @NotNull LocalDateTime startsAt,
        @NotNull @Min(1) @Max(100_000) Integer capacity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 7, fraction = 2) BigDecimal price,
        @Size(max = 500) @Pattern(regexp = "^(https?://\\S+)?$", message = "must be an http(s) URL") String imageUrl,
        @NotNull UUID categoryId
) {}
