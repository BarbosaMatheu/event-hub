package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.EventFilter;
import com.eventhub.event_hub.dtos.EventRequest;
import com.eventhub.event_hub.dtos.EventResponse;
import com.eventhub.event_hub.dtos.PageResponse;
import com.eventhub.event_hub.exceptions.BusinessException;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import com.eventhub.event_hub.repositories.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 6, 1, 10, 0);

    @Mock private EventRepository eventRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderItemRepository orderItemRepository;

    private EventService service;
    private final UUID categoryId = UUID.randomUUID();
    private Category category;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, categoryRepository, orderItemRepository,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        category = new Category("Tech");
    }

    private EventRequest request(LocalDateTime startsAt, int capacity) {
        return new EventRequest("  Spring Meetup ", "About Spring", " Hall 1 ", startsAt, capacity,
                new BigDecimal("50.00"), "", categoryId);
    }

    private Event existingEvent(int capacity, int sold) {
        Event event = new Event();
        event.setTitle("Old title");
        event.setLocation("Old place");
        event.setStartsAt(NOW.plusDays(10));
        event.setPrice(new BigDecimal("10.00"));
        event.setCategory(category);
        event.changeCapacity(capacity);
        if (sold > 0) {
            event.reserve(sold, NOW);
        }
        return event;
    }

    // ------------------------------------------------------------------ create

    @Test
    void createTrimsTextAndStampsTheCreationTime() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = service.create(request(NOW.plusDays(5), 100));

        assertEquals("Spring Meetup", response.title());
        assertEquals("Hall 1", response.location());
        assertEquals(100, response.availableTickets());
        assertNull(response.imageUrl());
        assertEquals(NOW, response.createdAt());
    }

    @Test
    void createRejectsEventsStartingInThePast() {
        assertThrows(BusinessException.class, () -> service.create(request(NOW.minusMinutes(1), 10)));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createFailsForUnknownCategory() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(request(NOW.plusDays(5), 10)));
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateChangesTheEventAndKeepsSoldTickets() {
        UUID id = UUID.randomUUID();
        Event event = existingEvent(50, 20);
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        EventResponse response = service.update(id, request(event.getStartsAt(), 80));

        assertEquals("Spring Meetup", response.title());
        assertEquals(80, response.capacity());
        assertEquals(20, response.ticketsSold());
        assertEquals(60, response.availableTickets());
    }

    @Test
    void updateCannotShrinkCapacityBelowSoldTickets() {
        UUID id = UUID.randomUUID();
        Event event = existingEvent(50, 20);
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThrows(BusinessException.class, () -> service.update(id, request(event.getStartsAt(), 10)));
        assertEquals(50, event.getCapacity());
    }

    @Test
    void updateCannotMoveTheEventIntoThePast() {
        UUID id = UUID.randomUUID();
        Event event = existingEvent(50, 0);
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThrows(BusinessException.class, () -> service.update(id, request(NOW.minusDays(1), 50)));
    }

    @Test
    void updateOfUnknownEventIsNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, request(NOW.plusDays(5), 10)));
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteIsRefusedWhenTheEventHasOrders() {
        UUID id = UUID.randomUUID();
        Event event = existingEvent(10, 1);
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(orderItemRepository.existsByEventId(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(id));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void deleteRemovesAnEventWithoutOrders() {
        UUID id = UUID.randomUUID();
        Event event = existingEvent(10, 0);
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(orderItemRepository.existsByEventId(id)).thenReturn(false);

        service.delete(id);

        verify(eventRepository).delete(event);
    }

    // ------------------------------------------------------------------ search / read

    @Test
    void findByIdOfUnknownEventIsNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void searchAppliesTheDefaultSortAndWrapsThePage() {
        Event event = existingEvent(10, 0);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        EventFilter filter = new EventFilter(null, null, null, null, null, null, false, false);

        PageResponse<EventResponse> page = service.search(filter, PageRequest.of(0, 12));

        assertEquals(1, page.content().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("startsAt").getDirection());
        assertNotNull(sort.getOrderFor("id"));
    }

    @Test
    void searchRejectsUnknownSortProperties() {
        EventFilter filter = new EventFilter(null, null, null, null, null, null, false, false);
        Pageable bad = PageRequest.of(0, 12, Sort.by("version"));

        assertThrows(BusinessException.class, () -> service.search(filter, bad));
        verifyNoInteractions(eventRepository);
    }
}
