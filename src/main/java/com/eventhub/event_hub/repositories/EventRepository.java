package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
}
