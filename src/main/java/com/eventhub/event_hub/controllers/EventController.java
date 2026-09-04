package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.EventRecordDto;
import com.eventhub.event_hub.dtos.EventResponseDto;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.services.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
    public ResponseEntity<EventResponseDto> saveEvent(@RequestBody @Valid EventRecordDto eventRecordDto) {
        Event event = eventService.save(eventRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponseDto.fromEntity(event));
    }

    // Read All (Paginated)
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<EventResponseDto>>> getAllEvents(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<Event> eventPage = eventService.findAll(pageable);

        Page<EntityModel<EventResponseDto>> dtoPage = eventPage.map(this::toEntityModel);
        PagedModel<EntityModel<EventResponseDto>> pagedModel = PagedModel.of(
                dtoPage.getContent(),
                new PagedModel.PageMetadata(dtoPage.getSize(), dtoPage.getNumber(), dtoPage.getTotalElements())
        );

        return ResponseEntity.status(HttpStatus.OK).body(pagedModel);
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EventResponseDto>> getOneEvent(@PathVariable(value = "id") UUID id) {
        Event event = eventService.findById(id);
        return ResponseEntity.status(HttpStatus.OK).body(toEntityModel(event));
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDto> updateEvent(@PathVariable(value = "id") UUID id, @RequestBody @Valid EventRecordDto eventRecordDto) {
        Event event = eventService.update(id, eventRecordDto);
        return ResponseEntity.status(HttpStatus.OK).body(EventResponseDto.fromEntity(event));
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable(value = "id") UUID id) {
        eventService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // Builds the DTO + HATEOAS links here in the web layer, keeping the
    // Event entity free of any web/HATEOAS concerns.
    private EntityModel<EventResponseDto> toEntityModel(Event event) {
        EventResponseDto dto = EventResponseDto.fromEntity(event);
        return EntityModel.of(dto,
                linkTo(methodOn(EventController.class).getOneEvent(event.getId())).withSelfRel(),
                linkTo(methodOn(EventController.class).getAllEvents(Pageable.unpaged())).withRel("events"));
    }
}
