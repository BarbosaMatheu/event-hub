package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.AbstractIntegrationTest;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against a real PostgreSQL with the Flyway migrations applied. This is what proves that the
 * entity mappings and the SQL migrations agree (Hibernate runs with ddl-auto=validate) and that
 * the database itself enforces the invariants the domain model relies on.
 */
class EventRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private JdbcTemplate jdbc;

    private Event newEvent(Category category, String title) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Integration test event");
        event.setLocation("Main Hall");
        event.setStartsAt(now.plusDays(10));
        event.setPrice(new BigDecimal("50.00"));
        event.setCategory(category);
        event.setCreatedAt(now);
        event.changeCapacity(100);
        return event;
    }

    @Test
    void flywayMigrationsWereActuallyApplied() {
        // Regression guard: with Spring Boot 4, a missing spring-boot-starter-flyway silently skips
        // every migration. This fails loudly if that ever happens again.
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true AND version IS NOT NULL", Integer.class);
        String latest = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL "
                        + "ORDER BY installed_rank DESC LIMIT 1", String.class);

        assertEquals(9, applied);
        assertEquals("9", latest);
    }

    @Test
    void savesAndLoadsAnEventTogetherWithItsCategory() {
        Category category = categoryRepository.save(new Category(unique("Tech-")));
        Event saved = eventRepository.save(newEvent(category, unique("Meetup-")));

        Optional<Event> found = eventRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        // The category is fetched by the entity graph, so this works outside a transaction.
        assertEquals(category.getName(), found.get().getCategory().getName());
        assertEquals(100, found.get().getAvailableTickets());
        assertNotNull(found.get().getVersion());
    }

    @Test
    void deletingAnEventDoesNotDeleteItsCategory() {
        Category category = categoryRepository.save(new Category(unique("Keep-")));
        Event saved = eventRepository.save(newEvent(category, unique("Workshop-")));

        eventRepository.delete(saved);

        assertTrue(categoryRepository.findById(category.getId()).isPresent());
    }

    @Test
    @Transactional
    void findByIdForUpdateReturnsTheRowUnderAWriteLock() {
        Category category = categoryRepository.save(new Category(unique("Lock-")));
        Event saved = eventRepository.save(newEvent(category, unique("Locked-")));

        Optional<Event> locked = eventRepository.findByIdForUpdate(saved.getId());

        assertTrue(locked.isPresent());
        assertEquals(saved.getId(), locked.get().getId());
    }

    @Test
    void theDatabaseItselfRefusesToOversellAnEvent() {
        Category category = categoryRepository.save(new Category(unique("Check-")));
        Event saved = eventRepository.save(newEvent(category, unique("Capped-")));

        // Bypass the domain model on purpose: even raw SQL cannot break the invariant.
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE TB_EVENTS SET tickets_sold = capacity + 1 WHERE id = ?", saved.getId()));
    }

    @Test
    void categoryNamesAreUniqueIgnoringCase() {
        String name = unique("Dup-");
        categoryRepository.saveAndFlush(new Category(name));

        assertThrows(DataIntegrityViolationException.class,
                () -> categoryRepository.saveAndFlush(new Category(name.toUpperCase())));
    }

    @Test
    void ticketStockChangesBumpTheVersionForOptimisticLocking() {
        Category category = categoryRepository.save(new Category(unique("Ver-")));
        Event saved = eventRepository.save(newEvent(category, unique("Versioned-")));
        Long before = saved.getVersion();

        Event loaded = eventRepository.findById(saved.getId()).orElseThrow();
        loaded.reserve(2, LocalDateTime.now(ZoneOffset.UTC));
        Event updated = eventRepository.saveAndFlush(loaded);

        assertEquals(2, updated.getTicketsSold());
        assertTrue(updated.getVersion() > before);
    }
}
