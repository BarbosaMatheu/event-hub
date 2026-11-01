package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.OrderItemRequest;
import com.eventhub.event_hub.dtos.OrderRequest;
import com.eventhub.event_hub.dtos.OrderResponse;
import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.exceptions.BusinessException;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.models.Order;
import com.eventhub.event_hub.models.OrderItem;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.EventRepository;
import com.eventhub.event_hub.repositories.OrderRepository;
import com.eventhub.event_hub.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 6, 1, 10, 0);
    private static final UUID EVENT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID EVENT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Mock private OrderRepository orderRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new OrderService(orderRepository, eventRepository, userRepository, clock, 30);
    }

    // ------------------------------------------------------------------ helpers

    private static Event event(UUID id, int capacity, String price, LocalDateTime startsAt) {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", id);
        event.setTitle("Event " + id.toString().substring(34));
        event.setStartsAt(startsAt);
        event.setPrice(new BigDecimal(price));
        event.changeCapacity(capacity);
        return event;
    }

    private static User actor(String id, boolean admin) {
        User user = mock(User.class);
        // lenient: which of these a given scenario touches depends on the code path taken
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getLogin()).thenReturn("login-" + id);
        lenient().when(user.isAdmin()).thenReturn(admin);
        return user;
    }

    /** A pending order owned by {@code owner}, holding {@code quantity} tickets of {@code event}. */
    private static Order pendingOrder(User owner, Event event, int quantity) {
        event.reserve(quantity, NOW);
        OrderItem item = new OrderItem();
        item.setEvent(event);
        item.setQuantity(quantity);
        item.setUnitPrice(event.getPrice());
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        order.setUser(owner);
        order.setCreatedAt(NOW.minusMinutes(5));
        order.addItem(item);
        return order;
    }

    private static OrderRequest request(OrderItemRequest... items) {
        return new OrderRequest(List.of(items));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createReservesTicketsAndSnapshotsThePrice() {
        User actor = actor("u1", false);
        Event event = event(EVENT_A, 10, "20.00", NOW.plusDays(3));
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.create(actor, request(new OrderItemRequest(EVENT_A, 3)));

        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(0, new BigDecimal("60.00").compareTo(response.totalAmount()));
        assertEquals(1, response.items().size());
        assertEquals(0, new BigDecimal("20.00").compareTo(response.items().get(0).unitPrice()));
        assertEquals(3, event.getTicketsSold());
        assertEquals(NOW.toInstant(ZoneOffset.UTC), response.createdAt());
        assertEquals(30 * 60L, response.expiresInSeconds(), "a brand-new pending order has the full TTL left");
    }

    @Test
    void createMergesDuplicateLinesOfTheSameEvent() {
        User actor = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(3));
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.create(actor,
                request(new OrderItemRequest(EVENT_A, 2), new OrderItemRequest(EVENT_A, 3)));

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(5, event.getTicketsSold());
        verify(eventRepository, times(1)).findByIdForUpdate(EVENT_A);
    }

    @Test
    void createLocksEventsInAscendingIdOrderRegardlessOfRequestOrder() {
        User actor = actor("u1", false);
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A))
                .thenReturn(Optional.of(event(EVENT_A, 10, "10.00", NOW.plusDays(1))));
        when(eventRepository.findByIdForUpdate(EVENT_B))
                .thenReturn(Optional.of(event(EVENT_B, 10, "10.00", NOW.plusDays(1))));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // The client asks for B first; the service must still lock A first (deadlock avoidance).
        service.create(actor, request(new OrderItemRequest(EVENT_B, 1), new OrderItemRequest(EVENT_A, 1)));

        InOrder order = inOrder(eventRepository);
        order.verify(eventRepository).findByIdForUpdate(EVENT_A);
        order.verify(eventRepository).findByIdForUpdate(EVENT_B);
    }

    @Test
    void createRejectsMoreThanTenTicketsForTheSameEvent() {
        User actor = actor("u1", false);
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));

        assertThrows(BusinessException.class, () -> service.create(actor,
                request(new OrderItemRequest(EVENT_A, 6), new OrderItemRequest(EVENT_A, 6))));

        verifyNoInteractions(eventRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createFailsForUnknownEvent() {
        User actor = actor("u1", false);
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(actor, request(new OrderItemRequest(EVENT_A, 1))));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createFailsWithConflictWhenThereAreNotEnoughTickets() {
        User actor = actor("u1", false);
        Event event = event(EVENT_A, 2, "10.00", NOW.plusDays(1));
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> service.create(actor, request(new OrderItemRequest(EVENT_A, 3))));

        assertEquals(0, event.getTicketsSold());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createFailsForEventsThatAlreadyStarted() {
        User actor = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.minusHours(1));
        when(userRepository.getReferenceById("u1")).thenReturn(new User("ann", "hash", UserRole.USER));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        assertThrows(BusinessException.class,
                () -> service.create(actor, request(new OrderItemRequest(EVENT_A, 1))));
    }

    // ------------------------------------------------------------------ cancel

    @Test
    void cancelReleasesTheTicketsBackToTheEvent() {
        User owner = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 4);
        UUID orderId = order.getId();
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        OrderResponse response = service.cancel(owner, orderId);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(0, event.getTicketsSold());
    }

    @Test
    void anotherUsersOrderLooksLikeItDoesNotExist() {
        User owner = actor("u1", false);
        User intruder = actor("u2", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 2);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class, () -> service.cancel(intruder, order.getId()));

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(2, event.getTicketsSold());
        verify(eventRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void anAdministratorCanCancelAnyPendingOrder() {
        User owner = actor("u1", false);
        User admin = actor("admin", true);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 2);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        service.cancel(admin, order.getId());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(0, event.getTicketsSold());
    }

    @Test
    void aCompletedOrderCannotBeCancelledAndKeepsItsTickets() {
        User owner = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 2);
        order.updateStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> service.cancel(owner, order.getId()));

        assertEquals(2, event.getTicketsSold());
        verify(eventRepository, never()).findByIdForUpdate(any());
    }

    // ------------------------------------------------------------------ pay / admin status

    @Test
    void payCompletesTheOwnersOrder() {
        User owner = actor("u1", false);
        Order order = pendingOrder(owner, event(EVENT_A, 10, "10.00", NOW.plusDays(2)), 1);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = service.pay(owner, order.getId());

        assertEquals(OrderStatus.COMPLETED, response.status());
    }

    @Test
    void onlyTheOwnerCanPay() {
        User owner = actor("u1", false);
        User other = actor("u2", false);
        Order order = pendingOrder(owner, event(EVENT_A, 10, "10.00", NOW.plusDays(2)), 1);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class, () -> service.pay(other, order.getId()));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void adminStatusChangeToCancelledAlsoReleasesTickets() {
        User owner = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 3);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        service.updateStatus(order.getId(), OrderStatus.CANCELLED);

        assertEquals(0, event.getTicketsSold());
    }

    // ------------------------------------------------------------------ queries

    @Test
    void findByIdHidesOtherUsersOrdersButAdminsSeeAll() {
        User owner = actor("u1", false);
        User other = actor("u2", false);
        User admin = actor("admin", true);
        Order order = pendingOrder(owner, event(EVENT_A, 10, "10.00", NOW.plusDays(2)), 1);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class, () -> service.findById(other, order.getId()));
        assertNotNull(service.findById(owner, order.getId()));
        assertNotNull(service.findById(admin, order.getId()));
    }

    // ------------------------------------------------------------------ expiration

    @Test
    void expiringAPendingOrderCancelsItAndReturnsTheTickets() {
        User owner = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 5);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(eventRepository.findByIdForUpdate(EVENT_A)).thenReturn(Optional.of(event));

        assertTrue(service.expireOrder(order.getId()));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(0, event.getTicketsSold());
    }

    @Test
    void expiringAnOrderThatWasPaidInTheMeantimeDoesNothing() {
        User owner = actor("u1", false);
        Event event = event(EVENT_A, 10, "10.00", NOW.plusDays(2));
        Order order = pendingOrder(owner, event, 5);
        order.updateStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertFalse(service.expireOrder(order.getId()));

        assertEquals(5, event.getTicketsSold());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void expiredOrdersAreThoseOlderThanTheTimeToLive() {
        when(orderRepository.findIdsByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.findExpiredOrderIds(100);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findIdsByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), cutoff.capture(), any(Pageable.class));
        assertEquals(NOW.minusMinutes(30), cutoff.getValue());
    }
}
