package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.AbstractIntegrationTest;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Runs against a real Postgres container (see AbstractIntegrationTest),
// with Flyway migrations applied exactly as they would be in production.
// This is what actually proves the entity mappings and the migration
// scripts agree with each other - something the Mockito-based unit tests
// (EventServiceTest, CategoryServiceTest) cannot verify by themselves.
class EventRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Tech");
        category = categoryRepository.save(category);
    }

    @Test
    void savesAndLoadsEventWithCategoryRelationship() {
        Event event = new Event();
        event.setTitle("Spring Boot Meetup");
        event.setDescription("A talk about Spring Boot");
        event.setLocal("Downtown Hall");
        event.setDateTime(LocalDateTime.now().plusDays(10));
        event.setMaximumCapacity(100);
        event.setPrice(new BigDecimal("50.00"));
        event.setCategory(category);

        Event saved = eventRepository.save(event);

        Optional<Event> found = eventRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Spring Boot Meetup", found.get().getTitle());
        assertEquals(category.getId(), found.get().getCategory().getId());
    }

    @Test
    void deletingEventDoesNotDeleteItsCategory() {
        Event event = new Event();
        event.setTitle("Workshop");
        event.setCategory(category);
        Event saved = eventRepository.save(event);

        eventRepository.delete(saved);

        assertTrue(categoryRepository.findById(category.getId()).isPresent());
    }
}
