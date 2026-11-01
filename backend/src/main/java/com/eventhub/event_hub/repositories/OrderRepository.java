package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.enums.OrderStatus;
import com.eventhub.event_hub.models.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Row lock used by every status change, so cancel/pay/expire can never race each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Query("select o.id from CustomerOrder o where o.status = :status and o.createdAt < :cutoff order by o.createdAt asc")
    List<UUID> findIdsByStatusAndCreatedAtBefore(@Param("status") OrderStatus status,
                                                 @Param("cutoff") LocalDateTime cutoff,
                                                 Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("select sum(o.totalAmount) from CustomerOrder o where o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);
}
