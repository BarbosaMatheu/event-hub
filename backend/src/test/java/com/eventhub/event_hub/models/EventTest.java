package com.eventhub.event_hub.models;

import com.eventhub.event_hub.exceptions.BusinessException;
import com.eventhub.event_hub.exceptions.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 6, 1, 12, 0);

    private static Event event(int capacity, LocalDateTime startsAt) {
        Event event = new Event();
        event.setTitle("Concert");
        event.setStartsAt(startsAt);
        event.changeCapacity(capacity);
        return event;
    }

    @Test
    void reservingTicketsReducesAvailability() {
        Event event = event(10, NOW.plusDays(5));

        event.reserve(4, NOW);

        assertEquals(4, event.getTicketsSold());
        assertEquals(6, event.getAvailableTickets());
    }

    @Test
    void canSellExactlyTheRemainingTickets() {
        Event event = event(3, NOW.plusDays(5));

        event.reserve(3, NOW);

        assertEquals(0, event.getAvailableTickets());
    }

    @Test
    void reservingMoreThanAvailableIsAConflictAndChangesNothing() {
        Event event = event(5, NOW.plusDays(5));
        event.reserve(4, NOW);

        ConflictException ex = assertThrows(ConflictException.class, () -> event.reserve(2, NOW));

        assertTrue(ex.getMessage().contains("available 1"));
        assertEquals(4, event.getTicketsSold());
    }

    @Test
    void cannotReserveAfterTheEventStarted() {
        Event event = event(5, NOW.minusMinutes(1));

        assertThrows(BusinessException.class, () -> event.reserve(1, NOW));
        assertEquals(0, event.getTicketsSold());
    }

    @Test
    void cannotReserveZeroOrNegativeQuantities() {
        Event event = event(5, NOW.plusDays(1));

        assertThrows(BusinessException.class, () -> event.reserve(0, NOW));
        assertThrows(BusinessException.class, () -> event.reserve(-2, NOW));
    }

    @Test
    void releasingGivesTicketsBackButNeverGoesBelowZero() {
        Event event = event(10, NOW.plusDays(1));
        event.reserve(3, NOW);

        event.release(2);
        assertEquals(1, event.getTicketsSold());

        event.release(50);
        assertEquals(0, event.getTicketsSold());
    }

    @Test
    void capacityCannotDropBelowTicketsAlreadySold() {
        Event event = event(10, NOW.plusDays(1));
        event.reserve(6, NOW);

        assertThrows(BusinessException.class, () -> event.changeCapacity(5));
        event.changeCapacity(6);
        assertEquals(6, event.getCapacity());
    }

    @Test
    void capacityMustBePositive() {
        Event event = new Event();
        assertThrows(BusinessException.class, () -> event.changeCapacity(0));
    }
}
