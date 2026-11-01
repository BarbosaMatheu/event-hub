package com.eventhub.event_hub.api;

import com.eventhub.event_hub.AbstractIntegrationTest;
import com.eventhub.event_hub.api.ApiClient.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCreatesARegularUserAndNeverEchoesThePassword() {
        String login = unique("Maria");

        Response response = api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null);

        assertEquals(201, response.status());
        assertEquals(login.toLowerCase(), response.str("login"), "logins are stored in lower case");
        assertEquals("USER", response.str("role"));
        assertFalse(response.contains("password"));
        assertFalse(response.contains(PASSWORD));
    }

    @Test
    void registerIgnoresAnAttemptToSelfAssignTheAdminRole() {
        String login = unique("sneaky");

        Response register = api().post("/api/v1/auth/register",
                "{\"login\":\"" + login + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}", null);
        assertEquals(201, register.status());
        assertEquals("USER", register.str("role"));

        String token = login(login, PASSWORD);
        assertEquals(403, api().get("/api/v1/admin/stats", token).status());
    }

    @Test
    void registeringTheSameLoginTwiceIsAConflictEvenWithDifferentCase() {
        String login = unique("dupe");
        assertEquals(201, api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null).status());

        Response again = api().post("/api/v1/auth/register", json("login", login.toUpperCase(), "password", PASSWORD), null);

        assertEquals(409, again.status());
    }

    @Test
    void registerValidatesTheRequestAndListsTheInvalidFields() {
        Response response = api().post("/api/v1/auth/register", json("login", "a b", "password", "short"), null);

        assertEquals(400, response.status());
        assertTrue(response.contains("\"violations\""));
        assertTrue(response.contains("\"login\""));
        assertTrue(response.contains("\"password\""));
    }

    @Test
    void loginReturnsAJwtThatIdentifiesTheCurrentUser() {
        String login = unique("ann");
        api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null);

        Response response = api().post("/api/v1/auth/login", json("login", login, "password", PASSWORD), null);

        assertEquals(200, response.status());
        assertEquals("Bearer", response.str("tokenType"));
        assertTrue(response.num("expiresInSeconds") > 0);
        String token = response.str("token");
        assertEquals(3, token.split("\\.").length, "a JWT has three parts");

        Response me = api().get("/api/v1/auth/me", token);
        assertEquals(200, me.status());
        assertEquals(login, me.str("login"));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameGenericAnswer() {
        String login = unique("known");
        api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null);

        Response wrongPassword = api().post("/api/v1/auth/login", json("login", login, "password", "WrongPassword1"), null);
        Response unknownUser = api().post("/api/v1/auth/login", json("login", unique("ghost"), "password", PASSWORD), null);

        assertEquals(401, wrongPassword.status());
        assertEquals(401, unknownUser.status());
        assertEquals(wrongPassword.str("message"), unknownUser.str("message"), "must not reveal which logins exist");
    }

    @Test
    void protectedEndpointsRequireAValidToken() {
        Response noToken = api().get("/api/v1/auth/me", null);
        assertEquals(401, noToken.status());
        assertTrue(noToken.contains("\"status\":401"), "401 uses the standard JSON error body");

        assertEquals(401, api().get("/api/v1/auth/me", "garbage.token.value").status());
        assertEquals(401, api().get("/api/v1/orders", null).status());
    }

    @Test
    void adminEndpointsAreForbiddenForRegularUsers() {
        String userToken = newUserToken();

        Response response = api().get("/api/v1/admin/stats", userToken);

        assertEquals(403, response.status());
        assertTrue(response.contains("\"status\":403"));
    }

    @Test
    void tooManyFailedLoginsAreThrottled() {
        String login = unique("brute");
        api().post("/api/v1/auth/register", json("login", login, "password", PASSWORD), null);

        for (int i = 0; i < 5; i++) {
            assertEquals(401, api().post("/api/v1/auth/login", json("login", login, "password", "Wrong-" + i + "-pass"), null).status());
        }

        Response blocked = api().post("/api/v1/auth/login", json("login", login, "password", PASSWORD), null);
        assertEquals(429, blocked.status(), "even the correct password is refused while blocked");
        assertTrue(blocked.headers().firstValue("Retry-After").isPresent());
    }

    @Test
    void malformedJsonIsABadRequestNotAServerError() {
        Response response = api().post("/api/v1/auth/login", "{not json", null);

        assertEquals(400, response.status());
        assertTrue(response.contains("\"status\":400"));
    }

    @Test
    void everyResponseCarriesARequestIdAndEchoesAValidOne() {
        Response generated = api().get("/api/v1/categories", null);
        assertTrue(generated.headers().firstValue("X-Request-Id").isPresent());

        Response echoed = api().send("GET", "/api/v1/categories", null, null, "X-Request-Id", "trace-abc-123");
        assertEquals("trace-abc-123", echoed.headers().firstValue("X-Request-Id").orElse(null));
    }

    @Test
    void healthEndpointIsPublic() {
        Response response = api().get("/actuator/health", null);

        assertEquals(200, response.status());
        assertTrue(response.contains("\"status\":\"UP\""));
    }

    @Test
    void unknownRoutesReturnTheStandardJsonError() {
        String token = newUserToken();

        Response response = api().get("/api/v1/this-does-not-exist", token);

        assertEquals(404, response.status());
        assertTrue(response.contains("\"status\":404"));
    }
}
