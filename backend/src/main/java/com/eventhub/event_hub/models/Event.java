package com.eventhub.event_hub.models;

import com.eventhub.event_hub.exceptions.BusinessException;
import com.eventhub.event_hub.exceptions.ConflictException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An event with a limited number of tickets.
 *
 * <p>Ticket accounting lives here, not in the services: {@link #reserve} and {@link #release}
 * are the only ways to change {@code ticketsSold}, so the invariant
 * {@code 0 <= ticketsSold <= capacity} is enforced in one place (and again by a CHECK
 * constraint in the database).
 */
@Entity
@Table(name = "TB_EVENTS")
@Getter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false, length = 255)
    private String title;

    @Setter
    @Column(length = 2000)
    private String description;

    @Setter
    @Column(nullable = false, length = 255)
    private String location;

    /** Venue-local wall-clock time (no timezone). */
    @Setter
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "tickets_sold", nullable = false)
    private int ticketsSold;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Optimistic locking guard for concurrent admin edits. */
    @Version
    private Long version;

    @Setter
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public int getAvailableTickets() {
        return capacity - ticketsSold;
    }

    public boolean hasStarted(LocalDateTime now) {
        return !startsAt.isAfter(now);
    }

    /** Sets the capacity; it can never drop below the tickets already sold. */
    public void changeCapacity(int newCapacity) {
        if (newCapacity < 1) {
            throw new BusinessException("Capacity must be at least 1");
        }
        if (newCapacity < ticketsSold) {
            throw new BusinessException("Capacity cannot be lower than the " + ticketsSold + " tickets already sold");
        }
        this.capacity = newCapacity;
    }

    /** Takes {@code quantity} tickets out of stock. Callers must hold a write lock on this row. */
    public void reserve(int quantity, LocalDateTime now) {
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }
        if (hasStarted(now)) {
            throw new BusinessException("Tickets for '" + title + "' are no longer available: the event has already started");
        }
        if (quantity > getAvailableTickets()) {
            throw new ConflictException("Not enough tickets for '" + title + "': requested " + quantity
                    + ", available " + getAvailableTickets());
        }
        ticketsSold += quantity;
    }

    /** Puts {@code quantity} tickets back in stock (cancelled or expired order). */
    public void release(int quantity) {
        ticketsSold = Math.max(0, ticketsSold - quantity);
    }
}
