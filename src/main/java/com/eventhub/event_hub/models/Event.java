package com.eventhub.event_hub.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Note: this entity intentionally does NOT extend HATEOAS's RepresentationModel.
// Mixing a persistence entity with a web/HATEOAS concern couples layers that
// should stay independent. Links are now built in the controller around
// EventResponseDto instead (see EventController).
@Entity
@Table(name = "TB_EVENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;
    private String description;
    private LocalDateTime dateTime;

    private String local;
    private Integer maximumCapacity;
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
