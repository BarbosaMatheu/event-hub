package com.eventhub.event_hub.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Optional criteria for the public event search. Every field may be null/false. */
public record EventFilter(
        String search,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        LocalDate from,
        LocalDate to,
        boolean onlyAvailable,
        boolean includePast
) {}
