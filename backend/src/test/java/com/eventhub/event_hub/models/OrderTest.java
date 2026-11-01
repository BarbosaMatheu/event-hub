package com.eventhub.event_hub.models;

import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTest {

    private static OrderItem item(String unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void itemSubtotalIsUnitPriceTimesQuantity() {
        assertEquals(0, new BigDecimal("59.70").compareTo(item("19.90", 3).getSubtotal()));
    }

    @Test
    void itemSubtotalIsZeroWhenDataIsMissing() {
        assertEquals(BigDecimal.ZERO, new OrderItem().getSubtotal());
    }

    @Test
    void addingItemsKeepsTheTotalUpToDate() {
        Order order = new Order();

        order.addItem(item("10.00", 2));
        order.addItem(item("5.00", 1));

        assertEquals(2, order.getItems().size());
        assertEquals(0, new BigDecimal("25.00").compareTo(order.getTotalAmount()));
    }

    @Test
    void newOrdersStartPending() {
        assertEquals(OrderStatus.PENDING, new Order().getStatus());
    }

    @Test
    void pendingOrderCanBeCompleted() {
        Order order = new Order();
        order.updateStatus(OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void completedOrderCannotBeCancelled() {
        Order order = new Order();
        order.updateStatus(OrderStatus.COMPLETED);

        assertThrows(BusinessException.class, () -> order.updateStatus(OrderStatus.CANCELLED));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void ownershipIsDecidedByUserId() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn("user-1");
        Order order = new Order();
        order.setUser(owner);

        assertTrue(order.isOwnedBy("user-1"));
        assertFalse(order.isOwnedBy("user-2"));
    }

    @Test
    void orderWithoutUserIsOwnedByNobody() {
        assertFalse(new Order().isOwnedBy("user-1"));
    }
}
