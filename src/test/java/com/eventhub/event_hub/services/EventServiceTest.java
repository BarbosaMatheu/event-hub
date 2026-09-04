package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.EventRecordDto;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private EventService eventService;

    private UUID categoryId;
    private Category category;
    private EventRecordDto dto;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new Category();
        category.setId(categoryId);
        category.setName("Tech");

        dto = new EventRecordDto(
                "Spring Boot Meetup",
                "A talk about Spring Boot",
                "Downtown Hall",
                LocalDateTime.now().plusDays(10),
                100,
                new BigDecimal("50.00"),
                categoryId
        );
    }

    @Test
    void saveThrowsWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.save(dto));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void saveCreatesEventWithResolvedCategory() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = eventService.save(dto);

        assertEquals(dto.title(), saved.getTitle());
        assertEquals(category, saved.getCategory());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void findByIdThrowsWhenEventDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.findById(id));
    }

    @Test
    void deleteRemovesExistingEvent() {
        UUID id = UUID.randomUUID();
        Event event = new Event();
        event.setId(id);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        eventService.delete(id);

        verify(eventRepository).delete(event);
    }
}
