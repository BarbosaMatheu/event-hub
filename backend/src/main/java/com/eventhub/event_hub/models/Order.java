package com.eventhub.event_hub.models;

import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A ticket order. The JPA entity name is "CustomerOrder" because ORDER is a reserved word
 * in JPQL/SQL; the table and the Java class keep the natural names.
 */
@Entity(name = "CustomerOrder")
@Table(name = "TB_ORDERS")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Setter
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
        calculateTotalAmount();
    }

    public void updateStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessException("Invalid status transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public boolean isOwnedBy(String userId) {
        return user != null && user.getId().equals(userId);
    }
}
