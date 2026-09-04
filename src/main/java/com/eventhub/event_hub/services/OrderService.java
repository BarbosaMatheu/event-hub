package com.eventhub.event_hub.services;

import org.springframework.stereotype.Service;
import com.eventhub.event_hub.repositories.OrderRepository;
import com.eventhub.event_hub.models.Order;
import com.eventhub.event_hub.models.OrderItem;
import com.eventhub.event_hub.dtos.OrderItemRequestDto;
import com.eventhub.event_hub.dtos.OrderResponseDto;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;

import com.eventhub.event_hub.enums.OrderStatus;

import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponseDto createOrder(List<OrderItemRequestDto> itemDtos) {
        Order order = new Order();

        for (OrderItemRequestDto itemDto : itemDtos) {
            OrderItem item = new OrderItem();
            item.setPrice(itemDto.price());
            item.setQuantity(itemDto.quantity());
            order.addItem(item);
        }
        Order savedOrder = orderRepository.save(order);
        return OrderResponseDto.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto findById(UUID id) {
        Order order = findOrderOrThrow(id);
        return OrderResponseDto.fromEntity(order);
    }

    @Transactional
    public OrderResponseDto updateStatus(UUID id, OrderStatus newStatus) {
        Order order = findOrderOrThrow(id);
        order.updateStatus(newStatus);
        return OrderResponseDto.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDto deleteOrder(UUID id) {
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
        return OrderResponseDto.fromEntity(order);
    }

    // Centralized lookup so every method throws the same, handled exception
    // type instead of a bare RuntimeException that GlobalExceptionHandler
    // doesn't recognize.
    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
}
