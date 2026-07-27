package com.eventhub.event_hub.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRecordDto(@NotBlank String title,  String description, String local, LocalDateTime dateTime, @NotNull Integer maximumCapacity, @NotNull BigDecimal price) {


}
