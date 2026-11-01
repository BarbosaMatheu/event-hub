package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.AdminStatsResponse;
import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import com.eventhub.event_hub.repositories.OrderRepository;
import com.eventhub.event_hub.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AdminStatsService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AdminStatsService(EventRepository eventRepository,
                             CategoryRepository categoryRepository,
                             OrderRepository orderRepository,
                             UserRepository userRepository,
                             Clock clock) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        Long sold = eventRepository.sumTicketsSold();
        BigDecimal revenue = orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED);

        var topEvents = eventRepository.findTop5ByOrderByTicketsSoldDescStartsAtAsc().stream()
                .map(e -> new AdminStatsResponse.TopEvent(
                        e.getId(), e.getTitle(), e.getTicketsSold(), e.getCapacity(), e.getPrice()))
                .toList();

        return new AdminStatsResponse(
                eventRepository.count(),
                eventRepository.countByStartsAtAfter(LocalDateTime.now(clock)),
                categoryRepository.count(),
                userRepository.count(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                sold == null ? 0 : sold,
                revenue == null ? BigDecimal.ZERO : revenue,
                topEvents);
    }
}
