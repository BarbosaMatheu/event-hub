package com.eventhub.event_hub.api;

import com.eventhub.event_hub.AbstractIntegrationTest;
import com.eventhub.event_hub.api.ApiClient.Response;
import com.eventhub.event_hub.jobs.PendingOrderExpirationJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OrderApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PendingOrderExpirationJob expirationJob;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private String eventWith(String admin, int capacity, String price) {
        String categoryId = createCategory(admin);
        Response created = createEvent(admin, categoryId, unique("Show-"), capacity, price, 10);
        assertEquals(201, created.status(), created.body());
        return created.str("id");
    }

    private long ticketsSold(String eventId) {
        return api().get("/api/v1/events/" + eventId, null).num("ticketsSold");
    }

    // ------------------------------------------------------------------ purchasing

    @Test
    void buyingTicketsReservesThemAndComputesTheTotalOnTheServer() {
        String admin = newAdminToken();
        String eventId = eventWith(admin, 10, "20.00");

        Response order = api().post("/api/v1/orders", orderJson(eventId, 3), newUserToken());

        assertEquals(201, order.status(), order.body());
        assertEquals("PENDING", order.str("status"));
        assertEquals(0, new BigDecimal("60.00").compareTo(order.dec("totalAmount")));
        assertEquals(3L, ticketsSold(eventId));
        assertEquals(7L, api().get("/api/v1/events/" + eventId, null).num("availableTickets"));
    }

    @Test
    void ordersRequireAuthentication() {
        String eventId = eventWith(newAdminToken(), 10, "5.00");

        assertEquals(401, api().post("/api/v1/orders", orderJson(eventId, 1), null).status());
    }

    @Test
    void theUnitPriceIsASnapshotAndSurvivesLaterPriceChanges() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String eventId = createEvent(admin, categoryId, unique("Price-"), 10, "20.00", 10).str("id");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 1), user).str("id");

        api().put("/api/v1/events/" + eventId, eventJson("Price changed", categoryId, 10, "99.00", 10), admin);

        Response order = api().get("/api/v1/orders/" + orderId, user);
        assertEquals(0, new BigDecimal("20.00").compareTo(order.dec("unitPrice")));
        assertEquals(0, new BigDecimal("20.00").compareTo(order.dec("totalAmount")));
    }

    @Test
    void buyingMoreThanAvailableIsRejectedAndChangesNothing() {
        String eventId = eventWith(newAdminToken(), 3, "5.00");

        Response response = api().post("/api/v1/orders", orderJson(eventId, 4), newUserToken());

        assertEquals(409, response.status());
        assertTrue(response.contains("\"status\":409"));
        assertEquals(0L, ticketsSold(eventId));
    }

    @Test
    void invalidOrdersAreRejected() {
        String user = newUserToken();
        String eventId = eventWith(newAdminToken(), 10, "5.00");

        assertEquals(400, api().post("/api/v1/orders", "{\"items\":[]}", user).status());
        assertEquals(400, api().post("/api/v1/orders", orderJson(eventId, 0), user).status());
        assertEquals(400, api().post("/api/v1/orders", orderJson(eventId, 11), user).status());
        assertEquals(404, api().post("/api/v1/orders", orderJson(UUID.randomUUID().toString(), 1), user).status());
    }

    @Test
    void anOrderCanContainSeveralEventsAndIsAtomic() {
        String admin = newAdminToken();
        String first = eventWith(admin, 10, "10.00");
        String second = eventWith(admin, 1, "30.00");
        String user = newUserToken();
        String body = "{\"items\":[{\"eventId\":\"" + first + "\",\"quantity\":2},{\"eventId\":\"" + second + "\",\"quantity\":1}]}";

        Response ok = api().post("/api/v1/orders", body, user);
        assertEquals(201, ok.status(), ok.body());
        assertEquals(0, new BigDecimal("50.00").compareTo(ok.dec("totalAmount")));

        // The second event is now sold out, so this whole order must fail without touching the first.
        Response fails = api().post("/api/v1/orders", body, newUserToken());
        assertEquals(409, fails.status());
        assertEquals(2L, ticketsSold(first), "all-or-nothing: no tickets of the first event were taken");
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    void concurrentBuyersNeverOversellTheLastTickets() throws Exception {
        String eventId = eventWith(newAdminToken(), 5, "10.00");
        int buyers = 12;
        List<String> tokens = IntStream.range(0, buyers).mapToObj(i -> newUserToken()).toList();

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String token : tokens) {
            results.add(pool.submit(() -> {
                startGate.await();
                return api().post("/api/v1/orders", orderJson(eventId, 1), token).status();
            }));
        }
        startGate.countDown(); // everybody fires at the same moment

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> result : results) {
            int status = result.get(60, TimeUnit.SECONDS);
            if (status == 201) {
                created++;
            } else if (status == 409) {
                conflicts++;
            } else {
                fail("Unexpected status " + status);
            }
        }
        pool.shutdown();

        assertEquals(5, created, "exactly the 5 available tickets were sold");
        assertEquals(7, conflicts, "everyone else was told the event is sold out");
        assertEquals(5L, ticketsSold(eventId));
        assertEquals(0L, api().get("/api/v1/events/" + eventId, null).num("availableTickets"));
    }

    // ------------------------------------------------------------------ lifecycle

    @Test
    void cancellingAPendingOrderReturnsTheTickets() {
        String eventId = eventWith(newAdminToken(), 10, "10.00");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 4), user).str("id");
        assertEquals(4L, ticketsSold(eventId));

        Response cancelled = api().post("/api/v1/orders/" + orderId + "/cancel", null, user);

        assertEquals(200, cancelled.status());
        assertEquals("CANCELLED", cancelled.str("status"));
        assertEquals(0L, ticketsSold(eventId));
        assertEquals(400, api().post("/api/v1/orders/" + orderId + "/cancel", null, user).status(),
                "cancelling twice must not release the tickets twice");
        assertEquals(0L, ticketsSold(eventId));
    }

    @Test
    void aPaidOrderIsFinalAndKeepsItsTickets() {
        String eventId = eventWith(newAdminToken(), 10, "10.00");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 2), user).str("id");

        Response paid = api().post("/api/v1/orders/" + orderId + "/pay", null, user);

        assertEquals(200, paid.status());
        assertEquals("COMPLETED", paid.str("status"));
        assertEquals(400, api().post("/api/v1/orders/" + orderId + "/cancel", null, user).status());
        assertEquals(400, api().post("/api/v1/orders/" + orderId + "/pay", null, user).status());
        assertEquals(2L, ticketsSold(eventId));
    }

    @Test
    void usersOnlySeeAndTouchTheirOwnOrders() {
        String eventId = eventWith(newAdminToken(), 10, "10.00");
        String owner = newUserToken();
        String stranger = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 1), owner).str("id");

        assertEquals(200, api().get("/api/v1/orders/" + orderId, owner).status());
        assertEquals(404, api().get("/api/v1/orders/" + orderId, stranger).status());
        assertEquals(404, api().post("/api/v1/orders/" + orderId + "/cancel", null, stranger).status());
        assertEquals(404, api().post("/api/v1/orders/" + orderId + "/pay", null, stranger).status());
        assertEquals("PENDING", api().get("/api/v1/orders/" + orderId, owner).str("status"));
    }

    @Test
    void myOrdersListsOnlyMyOrdersNewestFirst() {
        String eventId = eventWith(newAdminToken(), 20, "1.00");
        String alice = newUserToken();
        String bob = newUserToken();
        api().post("/api/v1/orders", orderJson(eventId, 1), alice);
        String newest = api().post("/api/v1/orders", orderJson(eventId, 2), alice).str("id");
        api().post("/api/v1/orders", orderJson(eventId, 1), bob);

        Response mine = api().get("/api/v1/orders", alice);

        assertEquals(200, mine.status());
        assertEquals(2L, mine.num("totalElements"));
        assertEquals(newest, mine.str("id"), "the most recent order comes first");
    }

    // ------------------------------------------------------------------ admin

    @Test
    void adminsSeeEveryOrderAndCanChangeStatuses() {
        String admin = newAdminToken();
        String eventId = eventWith(admin, 10, "10.00");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 3), user).str("id");

        assertEquals(200, api().get("/api/v1/orders/" + orderId, admin).status());
        assertTrue(api().get("/api/v1/admin/orders?status=PENDING", admin).contains(orderId));
        assertEquals(403, api().put("/api/v1/orders/" + orderId + "/status", json("status", "COMPLETED"), user).status());

        Response cancelled = api().put("/api/v1/orders/" + orderId + "/status", json("status", "CANCELLED"), admin);
        assertEquals(200, cancelled.status());
        assertEquals(0L, ticketsSold(eventId), "an admin cancellation also returns the tickets");
        assertEquals(400, api().put("/api/v1/orders/" + orderId + "/status", json("status", "COMPLETED"), admin).status());
    }

    @Test
    void dashboardStatsReflectTheActivity() {
        String admin = newAdminToken();
        String eventId = eventWith(admin, 10, "10.00");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 2), user).str("id");
        api().post("/api/v1/orders/" + orderId + "/pay", null, user);

        Response stats = api().get("/api/v1/admin/stats", admin);

        assertEquals(200, stats.status(), stats.body());
        assertTrue(stats.num("totalEvents") >= 1);
        assertTrue(stats.num("ticketsSold") >= 2);
        assertTrue(stats.dec("revenue").compareTo(new BigDecimal("20.00")) >= 0);
        assertTrue(stats.contains("\"topEvents\""));
    }

    // ------------------------------------------------------------------ expiration

    @Test
    void unpaidOrdersExpireAndReleaseTheirTickets() {
        String eventId = eventWith(newAdminToken(), 10, "10.00");
        String user = newUserToken();
        String orderId = api().post("/api/v1/orders", orderJson(eventId, 5), user).str("id");
        assertEquals(5L, ticketsSold(eventId));

        // Pretend the order was created two hours ago (the time-to-live is 30 minutes).
        jdbc.update("UPDATE TB_ORDERS SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now(clock).minusHours(2)), UUID.fromString(orderId));

        expirationJob.expirePendingOrders();

        assertEquals("CANCELLED", api().get("/api/v1/orders/" + orderId, user).str("status"));
        assertEquals(0L, ticketsSold(eventId));
    }

    @Test
    void recentAndPaidOrdersAreNotExpired() {
        String eventId = eventWith(newAdminToken(), 10, "10.00");
        String user = newUserToken();
        String recent = api().post("/api/v1/orders", orderJson(eventId, 1), user).str("id");
        String paid = api().post("/api/v1/orders", orderJson(eventId, 1), user).str("id");
        api().post("/api/v1/orders/" + paid + "/pay", null, user);
        jdbc.update("UPDATE TB_ORDERS SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now(clock).minusHours(2)), UUID.fromString(paid));

        expirationJob.expirePendingOrders();

        assertEquals("PENDING", api().get("/api/v1/orders/" + recent, user).str("status"));
        assertEquals("COMPLETED", api().get("/api/v1/orders/" + paid, user).str("status"));
        assertEquals(2L, ticketsSold(eventId));
    }
}
