package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.EventRecordDto;
import com.eventhub.event_hub.models.Event;
import org.springframework.http.ResponseEntity;
import com.eventhub.event_hub.repositories.EventRepository;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.UUID;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class EventController {
    
    @Autowired
    EventRepository eventRepository;

    // Create
    @PostMapping("/events")
    public ResponseEntity<Event> saveEvent(@RequestBody @Valid EventRecordDto eventRecordDto) {
        var event = new Event();
        BeanUtils.copyProperties(eventRecordDto, event);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventRepository.save(event));
    }

    // Read 
    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> eventList = eventRepository.findAll();
        if (!eventList.isEmpty()) {
            for (Event event : eventList) {
                event.add(linkTo(methodOn(EventController.class).getOneEvent(event.getId())).withSelfRel());
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(eventList);
    }

    // Read One
    @GetMapping("/events/{id}")
    public ResponseEntity<Event> getOneEvent(@PathVariable(value = "id") UUID id) {
        Optional<Event> eventO = eventRepository.findById(id);
        if (eventO.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); 
        }
        eventO.get().add(linkTo(methodOn(EventController.class).getAllEvents()).withRel("Events List"));
        return ResponseEntity.status(HttpStatus.OK).body(eventO.get());
    }

    // Update
    @PutMapping("/events/{id}")
    public ResponseEntity<Object> updateEvent(@PathVariable(value = "id") UUID id, @RequestBody @Valid EventRecordDto eventRecordDto) {
        Optional<Event> eventO = eventRepository.findById(id);
        if (eventO.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event not found");
        }
        var event = eventO.get();
        BeanUtils.copyProperties(eventRecordDto, event);
        return ResponseEntity.status(HttpStatus.OK).body(eventRepository.save(event));
    }

    // Delete
    @DeleteMapping("/events/{id}")
    public ResponseEntity<Object> deleteEvent(@PathVariable(value = "id") UUID id) {
        Optional<Event> eventO = eventRepository.findById(id);
        if (eventO.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event not found");
        }
        eventRepository.delete(eventO.get());
        return ResponseEntity.status(HttpStatus.OK).body("Event deleted successfully");
    }
}
