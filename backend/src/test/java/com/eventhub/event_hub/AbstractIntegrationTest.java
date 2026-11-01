package com.eventhub.event_hub;

import com.eventhub.event_hub.api.ApiClient;
import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for tests that need the whole application: real HTTP server, real security chain
 * and a real PostgreSQL started by Testcontainers (so the Flyway migrations are what is tested,
 * not a hand-made schema). Every subclass shares one Spring context and one database, so tests
 * create their own uniquely named data instead of assuming an empty database.
 *
 * <p>The container is started eagerly in a static initializer, so it is guaranteed to accept
 * connections before Spring builds the DataSource; Testcontainers' Ryuk reaper removes it when
 * the JVM exits.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password123!";

    static final PostgreSQLContainer<?> postgres;

    static {
        @SuppressWarnings("resource")
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("eventhub_test")
                .withUsername("test")
                .withPassword("test");
        container.start();
        postgres = container;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("api.security.token.secret", () -> "integration-test-secret-not-for-production-32chars");
        // Flyway migrations + ddl-auto=validate (from application.properties) stay ON on purpose:
        // the schema under test is the one the migrations build, and Hibernate must accept it.
    }

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected ApiClient api() {
        return new ApiClient("http://localhost:" + port);
    }

    // ------------------------------------------------------------------ data helpers

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append('"');
        }
        return sb.append('}').toString();
    }

    protected String login(String login, String password) {
        ApiClient.Response response = api().post("/api/v1/auth/login", json("login", login, "password", password), null);
        assertEquals(200, response.status(), "login failed: " + response.body());
        return response.str("token");
    }

    /** Registers a new regular user through the public API and returns a valid token. */
    protected String newUserToken() {
        String login = unique("user");
        ApiClient.Response response = api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null);
        assertEquals(201, response.status(), "register failed: " + response.body());
        return login(login, PASSWORD);
    }

    /** Admins cannot self-register, so the test inserts one directly and logs in through the API. */
    protected String newAdminToken() {
        String login = unique("admin");
        userRepository.save(new User(login, passwordEncoder.encode(PASSWORD), UserRole.ADMIN));
        return login(login, PASSWORD);
    }

    protected String createCategory(String adminToken) {
        ApiClient.Response response = api().post("/api/v1/categories", json("name", unique("Cat-")), adminToken);
        assertEquals(201, response.status(), response.body());
        return response.str("id");
    }

    protected static String futureDate(int daysAhead) {
        return LocalDateTime.now(ZoneOffset.UTC).plusDays(daysAhead).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    protected static String eventJson(String title, String categoryId, int capacity, String price, int daysAhead) {
        return "{\"title\":\"" + title + "\",\"description\":\"A test event\",\"location\":\"Main Hall\","
                + "\"startsAt\":\"" + futureDate(daysAhead) + "\",\"capacity\":" + capacity
                + ",\"price\":" + price + ",\"categoryId\":\"" + categoryId + "\"}";
    }

    protected ApiClient.Response createEvent(String adminToken, String categoryId, String title,
                                             int capacity, String price, int daysAhead) {
        return api().post("/api/v1/events", eventJson(title, categoryId, capacity, price, daysAhead), adminToken);
    }

    protected static String orderJson(String eventId, int quantity) {
        return "{\"items\":[{\"eventId\":\"" + eventId + "\",\"quantity\":" + quantity + "}]}";
    }
}
