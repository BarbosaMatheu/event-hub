package com.eventhub.event_hub.utils;

import com.eventhub.event_hub.exceptions.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Clients choose the sort via ?sort=field,dir. Only whitelisted properties are accepted, so a
 * bad value is a clean 400 instead of a 500 from the persistence layer, and internal columns
 * cannot be probed. A final "id" sort makes page boundaries deterministic when values tie.
 */
public final class PageableSanitizer {

    private PageableSanitizer() {}

    public static Pageable sanitize(Pageable pageable, Set<String> allowedProperties, Sort defaultSort) {
        Sort requested = pageable.getSort().isUnsorted() ? defaultSort : pageable.getSort();

        for (Sort.Order order : requested) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BusinessException("Unsupported sort property '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", new TreeSet<>(allowedProperties)));
            }
        }

        boolean sortsById = requested.stream().anyMatch(o -> o.getProperty().equals("id"));
        Sort finalSort = sortsById ? requested : requested.and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);
    }
}
