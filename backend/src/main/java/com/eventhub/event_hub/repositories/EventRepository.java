package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.models.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    /** Loads the category in the same query (avoids one extra select per row). */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Event> findById(UUID id);

    /**
     * SELECT ... FOR UPDATE. Every operation that changes the ticket stock goes through this
     * method, so concurrent purchases of the last tickets are serialised by the database.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCategoryId(UUID categoryId);

    long countByStartsAtAfter(LocalDateTime dateTime);

    @Query("select sum(e.ticketsSold) from Event e")
    Long sumTicketsSold();

    @EntityGraph(attributePaths = "category")
    List<Event> findTop5ByOrderByTicketsSoldDescStartsAtAsc();
}
