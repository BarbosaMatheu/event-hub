package com.eventhub.event_hub.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void totalPriceMultipliesPriceByQuantity() {
        OrderItem item = new OrderItem();
        item.setPrice(new BigDecimal("19.90"));
        item.setQuantity(3);

        assertEquals(new BigDecimal("59.70"), item.getTotalPrice());
    }

    @Test
    void totalPriceIsZeroWhenPriceIsNull() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);

        assertEquals(BigDecimal.ZERO, item.getTotalPrice());
    }

    @Test
    void totalPriceIsZeroWhenQuantityIsNull() {
        OrderItem item = new OrderItem();
        item.setPrice(new BigDecimal("10.00"));

        assertEquals(BigDecimal.ZERO, item.getTotalPrice());
    }
}
