package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.EventRecordDto;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.services.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Create
    @PostMapping
    public ResponseEntity<Event> saveEvent(@RequestBody @Valid EventRecordDto eventRecordDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.save(eventRecordDto));
    }

    // Read All (Paginated)
    @GetMapping
    public ResponseEntity<Page<Event>> getAllEvents(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<Event> eventPage = eventService.findAll(pageable);

        if (!eventPage.isEmpty()) {
            for (Event event : eventPage.getContent()) {
                event.add(linkTo(methodOn(EventController.class).getOneEvent(event.getId())).withSelfRel());
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(eventPage);
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<Event> getOneEvent(@PathVariable(value = "id") UUID id) {
        Event event = eventService.findById(id);
        
        // Pass Pageable.unpaged() so HATEOAS knows which method signature to target
        event.add(linkTo(methodOn(EventController.class).getAllEvents(Pageable.unpaged())).withRel("Events List"));
        
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable(value = "id") UUID id, @RequestBody @Valid EventRecordDto eventRecordDto) {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.update(id, eventRecordDto));
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable(value = "id") UUID id) {
        eventService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}