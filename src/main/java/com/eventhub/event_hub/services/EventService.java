package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.EventRecordDto;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    // Constructor Injection for both repositories
    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Event save(EventRecordDto dto) {
        // Find category or throw exception handled by GlobalExceptionHandler
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.categoryId()));

        var event = new Event();
        BeanUtils.copyProperties(dto, event);
        event.setCategory(category);

        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<Event> findAll(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Event findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
    }

    @Transactional
    public Event update(UUID id, EventRecordDto dto) {
        Event event = findById(id);

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.categoryId()));

        BeanUtils.copyProperties(dto, event);
        event.setCategory(category);

        return eventRepository.save(event);
    }

    @Transactional
    public void delete(UUID id) {
        Event event = findById(id);
        eventRepository.delete(event);
    }
}