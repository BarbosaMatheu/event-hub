package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.EventFilter;
import com.eventhub.event_hub.dtos.EventRequest;
import com.eventhub.event_hub.dtos.EventResponse;
import com.eventhub.event_hub.dtos.PageResponse;
import com.eventhub.event_hub.services.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Public to browse, ADMIN to change")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Search events",
            description = "All filters are optional and combinable. Sort with ?sort=startsAt,asc "
                    + "(allowed: startsAt, price, title, createdAt). Past events are hidden unless includePast=true.")
    public PageResponse<EventResponse> search(
            @Parameter(description = "Matches title, description or location (case-insensitive)")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Events starting on or after this date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Events starting on or before this date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(defaultValue = "false") boolean includePast,
            @PageableDefault(size = 12) Pageable pageable) {

        EventFilter filter = new EventFilter(search, categoryId, minPrice, maxPrice, from, to, onlyAvailable, includePast);
        return eventService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id) {
        return eventService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> create(@RequestBody @Valid EventRequest request) {
        EventResponse created = eventService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/events/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse update(@PathVariable UUID id, @RequestBody @Valid EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an event", description = "409 if the event already has orders.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        eventService.delete(id);
    }
}
