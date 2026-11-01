package com.eventhub.event_hub.utils;

import com.eventhub.event_hub.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageableSanitizerTest {

    private static final Set<String> ALLOWED = Set.of("startsAt", "price");
    private static final Sort DEFAULT = Sort.by(Sort.Direction.ASC, "startsAt");

    @Test
    void usesDefaultSortAndAddsIdAsTieBreaker() {
        Pageable result = PageableSanitizer.sanitize(PageRequest.of(2, 12), ALLOWED, DEFAULT);

        assertEquals(2, result.getPageNumber());
        assertEquals(12, result.getPageSize());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("startsAt").getDirection());
        assertNotNull(result.getSort().getOrderFor("id"));
    }

    @Test
    void keepsAnAllowedRequestedSort() {
        Pageable requested = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "price"));

        Pageable result = PageableSanitizer.sanitize(requested, ALLOWED, DEFAULT);

        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("price").getDirection());
        assertNull(result.getSort().getOrderFor("startsAt"));
    }

    @Test
    void rejectsPropertiesOutsideTheWhitelist() {
        Pageable requested = PageRequest.of(0, 5, Sort.by("password"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> PageableSanitizer.sanitize(requested, ALLOWED, DEFAULT));

        assertTrue(ex.getMessage().contains("password"));
    }

    @Test
    void doesNotDuplicateIdWhenAlreadyRequested() {
        Set<String> allowed = Set.of("createdAt", "id");
        Pageable requested = PageRequest.of(0, 5, Sort.by("id"));

        Pageable result = PageableSanitizer.sanitize(requested, allowed, DEFAULT);

        assertEquals(1, result.getSort().stream().count());
    }
}
