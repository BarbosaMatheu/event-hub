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
import com.eventhub.event_hub.utils.EventSpecifications;
import com.eventhub.event_hub.utils.PageableSanitizer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

    private static final Set<String> SORTABLE = Set.of("startsAt", "price", "title", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "startsAt");

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        OrderItemRepository orderItemRepository,
                        Clock clock) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> search(EventFilter filter, Pageable pageable) {
        Pageable safe = PageableSanitizer.sanitize(pageable, SORTABLE, DEFAULT_SORT);
        var spec = EventSpecifications.matching(filter, LocalDateTime.now(clock));
        return PageResponse.from(eventRepository.findAll(spec, safe).map(EventResponse::from));
    }

    @Transactional(readOnly = true)
    public EventResponse findById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!request.startsAt().isAfter(now)) {
            throw new BusinessException("The event must start in the future");
        }
        Category category = findCategory(request.categoryId());

        Event event = new Event();
        apply(event, request, category);
        event.changeCapacity(request.capacity());
        event.setCreatedAt(now);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(UUID id, EventRequest request) {
        // Row lock: an admin edit and a ticket purchase never interleave on the same event.
        Event event = eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        Category category = findCategory(request.categoryId());

        boolean dateChanged = !request.startsAt().equals(event.getStartsAt());
        if (dateChanged && !request.startsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("The new start date must be in the future");
        }

        event.changeCapacity(request.capacity());
        apply(event, request, category);
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(UUID id) {
        Event event = eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        if (orderItemRepository.existsByEventId(id)) {
            throw new ConflictException("Cannot delete an event that already has orders");
        }
        eventRepository.delete(event);
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
    }

    private void apply(Event event, EventRequest request, Category category) {
        event.setTitle(request.title().trim());
        event.setDescription(blankToNull(request.description()));
        event.setLocation(request.location().trim());
        event.setStartsAt(request.startsAt());
        event.setPrice(request.price());
        event.setImageUrl(blankToNull(request.imageUrl()));
        event.setCategory(category);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
