package com.eventhub.event_hub.utils;

import com.eventhub.event_hub.dtos.EventFilter;
import com.eventhub.event_hub.models.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the dynamic WHERE clause of the public event search. Only the criteria that were
 * actually provided become predicates, so there are no "param IS NULL OR ..." tricks (which
 * misbehave with PostgreSQL parameter typing).
 */
public final class EventSpecifications {

    private EventSpecifications() {}

    public static Specification<Event> matching(EventFilter filter, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!filter.includePast()) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startsAt"), now));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("location")), pattern, '\\')));
            }

            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), filter.maxPrice()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startsAt"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("startsAt"), filter.to().plusDays(1).atStartOfDay()));
            }
            if (filter.onlyAvailable()) {
                predicates.add(cb.lessThan(root.<Integer>get("ticketsSold"), root.<Integer>get("capacity")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Makes %, _ and \ in user input literal characters inside a LIKE pattern. */
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
