package com.eventhub.event_hub.models;

import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void addingItemsRecalculatesTotalAmount() {
        Order order = new Order();

        OrderItem item1 = new OrderItem();
        item1.setPrice(new BigDecimal("10.00"));
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setPrice(new BigDecimal("5.00"));
        item2.setQuantity(1);

        order.addItem(item1);
        order.addItem(item2);

        assertEquals(new BigDecimal("25.00"), order.getTotalAmount());
        assertEquals(2, order.getItems().size());
    }

    @Test
    void updateStatusAllowsValidTransition() {
        Order order = new Order();

        order.updateStatus(OrderStatus.COMPLETED);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Order order = new Order();
        order.updateStatus(OrderStatus.COMPLETED);

        assertThrows(BusinessException.class, () -> order.updateStatus(OrderStatus.CANCELLED));
    }
}
