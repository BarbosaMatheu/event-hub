package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.OrderItemRequest;
import com.eventhub.event_hub.dtos.OrderRequest;
import com.eventhub.event_hub.dtos.OrderResponse;
import com.eventhub.event_hub.dtos.PageResponse;
import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.exceptions.BusinessException;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.models.Order;
import com.eventhub.event_hub.models.OrderItem;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.EventRepository;
import com.eventhub.event_hub.repositories.OrderRepository;
import com.eventhub.event_hub.repositories.UserRepository;
import com.eventhub.event_hub.utils.PageableSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Ticket purchases. The interesting part is concurrency: every code path that changes the
 * ticket stock locks the event rows (SELECT ... FOR UPDATE) in a fixed order (by id), so
 * two buyers can never oversell the last tickets and two transactions can never deadlock.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_TICKETS_PER_EVENT = 10;
    private static final Set<String> SORTABLE = Set.of("createdAt", "totalAmount", "status");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final long pendingTtlMinutes;

    public OrderService(OrderRepository orderRepository,
                        EventRepository eventRepository,
                        UserRepository userRepository,
                        Clock clock,
                        @Value("${app.orders.pending-ttl-minutes:30}") long pendingTtlMinutes) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.pendingTtlMinutes = pendingTtlMinutes;
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public OrderResponse create(User actor, OrderRequest request) {
        // Merge duplicate events and iterate in id order (TreeMap) => consistent lock order.
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.eventId(), item.quantity(), Integer::sum);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(actor.getId()));
        order.setCreatedAt(now);

        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            if (entry.getValue() > MAX_TICKETS_PER_EVENT) {
                throw new BusinessException("You can buy at most " + MAX_TICKETS_PER_EVENT + " tickets per event");
            }
            Event event = eventRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + entry.getKey()));
            event.reserve(entry.getValue(), now);

            OrderItem item = new OrderItem();
            item.setEvent(event);
            item.setQuantity(entry.getValue());
            item.setUnitPrice(event.getPrice());
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} created by {} (total {})", saved.getId(), actor.getLogin(), saved.getTotalAmount());
        return toResponse(saved);
    }

    /** Simulated payment: the owner completes their own pending order. */
    @Transactional
    public OrderResponse pay(User actor, UUID orderId) {
        Order order = lockOrder(orderId);
        if (!order.isOwnedBy(actor.getId())) {
            throw notFound(orderId);
        }
        order.updateStatus(OrderStatus.COMPLETED);
        log.info("Order {} paid by {}", orderId, actor.getLogin());
        return toResponse(order);
    }

    /** Owners can cancel their own pending orders; admins can cancel any pending order. */
    @Transactional
    public OrderResponse cancel(User actor, UUID orderId) {
        Order order = lockOrder(orderId);
        if (!actor.isAdmin() && !order.isOwnedBy(actor.getId())) {
            throw notFound(orderId);
        }
        applyStatus(order, OrderStatus.CANCELLED);
        log.info("Order {} cancelled by {}", orderId, actor.getLogin());
        return toResponse(order);
    }

    /** Admin override of the status (still validated by the status state machine). */
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = lockOrder(orderId);
        applyStatus(order, newStatus);
        log.info("Order {} moved to {} by an administrator", orderId, newStatus);
        return toResponse(order);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findMine(User actor, Pageable pageable) {
        Pageable safe = PageableSanitizer.sanitize(pageable, SORTABLE, DEFAULT_SORT);
        return PageResponse.from(orderRepository.findByUserId(actor.getId(), safe).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findAll(OrderStatus status, Pageable pageable) {
        Pageable safe = PageableSanitizer.sanitize(pageable, SORTABLE, DEFAULT_SORT);
        Page<Order> page = status == null
                ? orderRepository.findAll(safe)
                : orderRepository.findByStatus(status, safe);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(User actor, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> notFound(orderId));
        // Other people's orders look exactly like orders that do not exist.
        if (!actor.isAdmin() && !order.isOwnedBy(actor.getId())) {
            throw notFound(orderId);
        }
        return toResponse(order);
    }

    // ------------------------------------------------------------------ expiration of unpaid orders

    @Transactional(readOnly = true)
    public List<UUID> findExpiredOrderIds(int limit) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(pendingTtlMinutes);
        return orderRepository.findIdsByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff, PageRequest.of(0, limit));
    }

    /** Cancels one expired order and returns its tickets. Returns false if it is no longer pending. */
    @Transactional
    public boolean expireOrder(UUID orderId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            return false; // paid or cancelled in the meantime
        }
        applyStatus(order, OrderStatus.CANCELLED);
        log.info("Order {} expired after {} minutes without payment", orderId, pendingTtlMinutes);
        return true;
    }

    // ------------------------------------------------------------------ internals

    private Order lockOrder(UUID orderId) {
        return orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> notFound(orderId));
    }

    private void applyStatus(Order order, OrderStatus newStatus) {
        order.updateStatus(newStatus); // throws BusinessException for an invalid transition
        if (newStatus == OrderStatus.CANCELLED) {
            releaseTickets(order);
        }
    }

    private void releaseTickets(Order order) {
        List<OrderItem> sorted = order.getItems().stream()
                .sorted(Comparator.comparing((OrderItem item) -> item.getEvent().getId()))
                .toList();
        for (OrderItem item : sorted) {
            Event event = eventRepository.findByIdForUpdate(item.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + item.getEvent().getId()));
            event.release(item.getQuantity());
        }
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.from(order, clock, pendingTtlMinutes);
    }

    private static ResourceNotFoundException notFound(UUID orderId) {
        return new ResourceNotFoundException("Order not found with ID: " + orderId);
    }
}
