package com.eventhub.event_hub.api;

import com.eventhub.event_hub.AbstractIntegrationTest;
import com.eventhub.event_hub.api.ApiClient.Response;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class EventApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private CategoryRepository categoryRepository;

    // ------------------------------------------------------------------ permissions & validation

    @Test
    void browsingEventsIsPublic() {
        Response response = api().get("/api/v1/events", null);

        assertEquals(200, response.status());
        assertTrue(response.contains("\"content\""));
        assertTrue(response.contains("\"totalElements\""));
    }

    @Test
    void onlyAdminsCanCreateEvents() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String body = eventJson(unique("Forbidden-"), categoryId, 10, "5.00", 5);

        assertEquals(401, api().post("/api/v1/events", body, null).status());
        assertEquals(403, api().post("/api/v1/events", body, newUserToken()).status());
        assertEquals(201, api().post("/api/v1/events", body, admin).status());
    }

    @Test
    void createdEventHasAllTicketsAvailableAndALocationHeader() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);

        Response created = createEvent(admin, categoryId, unique("Concert-"), 80, "25.50", 7);

        assertEquals(201, created.status(), created.body());
        assertEquals(80L, created.num("capacity"));
        assertEquals(0L, created.num("ticketsSold"));
        assertEquals(80L, created.num("availableTickets"));
        assertEquals(0, new BigDecimal("25.50").compareTo(created.dec("price")));
        assertTrue(created.headers().firstValue("Location").orElse("").endsWith("/api/v1/events/" + created.str("id")));
    }

    @Test
    void invalidEventsAreRejectedWithFieldDetails() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String body = "{\"title\":\"\",\"location\":\"Hall\",\"startsAt\":\"" + futureDate(3)
                + "\",\"capacity\":0,\"price\":-1,\"categoryId\":\"" + categoryId + "\"}";

        Response response = api().post("/api/v1/events", body, admin);

        assertEquals(400, response.status());
        assertTrue(response.contains("\"title\""));
        assertTrue(response.contains("\"capacity\""));
        assertTrue(response.contains("\"price\""));
    }

    @Test
    void eventsCannotBeCreatedInThePastOrForUnknownCategories() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);

        assertEquals(400, createEvent(admin, categoryId, unique("Past-"), 10, "1.00", -2).status());
        assertEquals(404, createEvent(admin, "00000000-0000-0000-0000-000000000000", unique("NoCat-"), 10, "1.00", 2).status());
    }

    // ------------------------------------------------------------------ search

    @Test
    void searchCombinesTextCategoryPriceAndSorting() {
        String admin = newAdminToken();
        String catA = createCategory(admin);
        String catB = createCategory(admin);
        String token = unique("zq").toLowerCase();
        createEvent(admin, catA, "Cheap " + token, 10, "10.00", 5);
        createEvent(admin, catA, "Pricey " + token, 10, "50.00", 6);
        createEvent(admin, catB, "Medium " + token, 10, "30.00", 7);

        assertEquals(3L, api().get("/api/v1/events?search=" + token, null).num("totalElements"));
        assertEquals(2L, api().get("/api/v1/events?search=" + token + "&categoryId=" + catA, null).num("totalElements"));
        assertEquals(1L, api().get("/api/v1/events?search=" + token + "&minPrice=20&maxPrice=40", null).num("totalElements"));

        Response byPriceDesc = api().get("/api/v1/events?search=" + token + "&sort=price,desc", null);
        assertEquals(0, new BigDecimal("50.00").compareTo(byPriceDesc.dec("price")), "most expensive first");
        Response byPriceAsc = api().get("/api/v1/events?search=" + token + "&sort=price,asc", null);
        assertEquals(0, new BigDecimal("10.00").compareTo(byPriceAsc.dec("price")), "cheapest first");
    }

    @Test
    void searchTreatsPercentAndUnderscoreAsLiteralText() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        createEvent(admin, categoryId, unique("Plain-"), 10, "1.00", 4);

        // If "%" or "_" acted as LIKE wildcards, these searches would match every event.
        // No test data contains these characters, so the literal search finds nothing.
        assertEquals(0L, api().get("/api/v1/events?search=%25", null).num("totalElements"));
        assertEquals(0L, api().get("/api/v1/events?search=_", null).num("totalElements"));
    }

    @Test
    void searchFiltersByDateRange() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String token = unique("dt").toLowerCase();
        createEvent(admin, categoryId, "Soon " + token, 10, "1.00", 10);
        createEvent(admin, categoryId, "Later " + token, 10, "1.00", 20);
        LocalDate middle = LocalDate.now(ZoneOffset.UTC).plusDays(15);

        assertEquals(1L, api().get("/api/v1/events?search=" + token + "&from=" + middle, null).num("totalElements"));
        assertEquals(1L, api().get("/api/v1/events?search=" + token + "&to=" + middle, null).num("totalElements"));
    }

    @Test
    void pastEventsAreHiddenUnlessRequested() {
        String token = unique("old").toLowerCase();
        Category category = categoryRepository.save(new Category(unique("PastCat-")));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Event past = new Event();
        past.setTitle("Yesterday " + token);
        past.setLocation("Old Hall");
        past.setStartsAt(now.minusDays(2));
        past.setPrice(BigDecimal.TEN);
        past.setCategory(category);
        past.setCreatedAt(now.minusDays(30));
        past.changeCapacity(10);
        eventRepository.save(past);

        assertEquals(0L, api().get("/api/v1/events?search=" + token, null).num("totalElements"));
        assertEquals(1L, api().get("/api/v1/events?search=" + token + "&includePast=true", null).num("totalElements"));
    }

    @Test
    void onlyAvailableHidesSoldOutEvents() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String token = unique("so").toLowerCase();
        String soldOutId = createEvent(admin, categoryId, "Tiny " + token, 2, "1.00", 5).str("id");
        createEvent(admin, categoryId, "Big " + token, 50, "1.00", 6);

        assertEquals(201, api().post("/api/v1/orders", orderJson(soldOutId, 2), newUserToken()).status());

        assertEquals(2L, api().get("/api/v1/events?search=" + token, null).num("totalElements"));
        assertEquals(1L, api().get("/api/v1/events?search=" + token + "&onlyAvailable=true", null).num("totalElements"));
    }

    @Test
    void paginationSplitsResultsIntoPages() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String token = unique("pg").toLowerCase();
        for (int i = 0; i < 3; i++) {
            createEvent(admin, categoryId, "Item " + i + " " + token, 10, "1.00", 5 + i);
        }

        Response page = api().get("/api/v1/events?search=" + token + "&size=2&page=0", null);

        assertEquals(3L, page.num("totalElements"));
        assertEquals(2L, page.num("totalPages"));
        assertEquals(2L, page.num("size"));
    }

    @Test
    void unsupportedSortAndBadParametersAreCleanBadRequests() {
        assertEquals(400, api().get("/api/v1/events?sort=password,asc", null).status());
        assertEquals(400, api().get("/api/v1/events?categoryId=not-a-uuid", null).status());
        assertEquals(400, api().get("/api/v1/events?from=yesterday", null).status());
    }

    // ------------------------------------------------------------------ update / delete

    @Test
    void adminCanUpdateAnEvent() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String id = createEvent(admin, categoryId, unique("Before-"), 10, "10.00", 5).str("id");

        Response updated = api().put("/api/v1/events/" + id, eventJson("After rename", categoryId, 25, "12.00", 6), admin);

        assertEquals(200, updated.status(), updated.body());
        assertEquals("After rename", updated.str("title"));
        assertEquals(25L, updated.num("capacity"));
        assertEquals("After rename", api().get("/api/v1/events/" + id, null).str("title"));
    }

    @Test
    void capacityCannotBeReducedBelowTheTicketsAlreadySold() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String id = createEvent(admin, categoryId, unique("Sold-"), 10, "10.00", 5).str("id");
        assertEquals(201, api().post("/api/v1/orders", orderJson(id, 4), newUserToken()).status());

        Response tooSmall = api().put("/api/v1/events/" + id, eventJson("Sold", categoryId, 3, "10.00", 5), admin);

        assertEquals(400, tooSmall.status());
        assertEquals(4L, api().get("/api/v1/events/" + id, null).num("ticketsSold"));
    }

    @Test
    void anEventWithoutOrdersCanBeDeletedButOneWithOrdersCannot() {
        String admin = newAdminToken();
        String categoryId = createCategory(admin);
        String freeId = createEvent(admin, categoryId, unique("Free-"), 10, "1.00", 5).str("id");
        String soldId = createEvent(admin, categoryId, unique("Sold-"), 10, "1.00", 5).str("id");
        assertEquals(201, api().post("/api/v1/orders", orderJson(soldId, 1), newUserToken()).status());

        assertEquals(204, api().delete("/api/v1/events/" + freeId, admin).status());
        assertEquals(404, api().get("/api/v1/events/" + freeId, null).status());
        assertEquals(409, api().delete("/api/v1/events/" + soldId, admin).status());
        assertEquals(403, api().delete("/api/v1/events/" + soldId, newUserToken()).status());
    }

    // ------------------------------------------------------------------ categories

    @Test
    void categoriesSupportFullCrudForAdminsOnly() {
        String admin = newAdminToken();
        String name = unique("Music-");

        Response created = api().post("/api/v1/categories", json("name", name), admin);
        assertEquals(201, created.status());
        String id = created.str("id");

        assertEquals(403, api().post("/api/v1/categories", json("name", unique("X-")), newUserToken()).status());
        assertEquals(200, api().get("/api/v1/categories/" + id, null).status());
        assertTrue(api().get("/api/v1/categories", null).contains(name));

        assertEquals(200, api().put("/api/v1/categories/" + id, json("name", name + "-renamed"), admin).status());
        assertEquals(204, api().delete("/api/v1/categories/" + id, admin).status());
        assertEquals(404, api().get("/api/v1/categories/" + id, null).status());
    }

    @Test
    void categoryNamesMustBeUniqueAndUsedCategoriesCannotBeDeleted() {
        String admin = newAdminToken();
        String name = unique("Unique-");
        String id = api().post("/api/v1/categories", json("name", name), admin).str("id");

        assertEquals(409, api().post("/api/v1/categories", json("name", name.toUpperCase()), admin).status());

        createEvent(admin, id, unique("Uses-"), 10, "1.00", 5);
        assertEquals(409, api().delete("/api/v1/categories/" + id, admin).status());
    }
}
