package com.eventhub.event_hub.jobs;

import com.eventhub.event_hub.services.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Releases the tickets of orders that stayed unpaid for too long, so abandoned checkouts do not
 * block inventory. Each order is expired in its own transaction (via OrderService, so the
 * transactional proxy applies) and a failure in one never stops the others.
 */
@Component
public class PendingOrderExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PendingOrderExpirationJob.class);
    private static final int BATCH_SIZE = 100;

    private final OrderService orderService;

    public PendingOrderExpirationJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(
            initialDelayString = "${app.orders.expiration-check-interval-ms:60000}",
            fixedDelayString = "${app.orders.expiration-check-interval-ms:60000}")
    public void expirePendingOrders() {
        List<UUID> ids = orderService.findExpiredOrderIds(BATCH_SIZE);
        int expired = 0;
        for (UUID id : ids) {
            try {
                if (orderService.expireOrder(id)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not expire order {}: {}", id, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} unpaid order(s)", expired);
        }
    }
}
