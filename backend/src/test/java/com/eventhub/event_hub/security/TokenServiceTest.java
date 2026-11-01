package com.eventhub.event_hub.security;

import com.eventhub.event_hub.config.security.TokenService;
import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.models.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-bytes!!";

    private final User user = new User("ann", "hash", UserRole.ADMIN);

    @Test
    void generatedTokenValidatesAndReturnsTheLogin() {
        TokenService service = new TokenService(SECRET, 60);

        String token = service.generateToken(user);

        assertEquals("ann", service.validateToken(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        TokenService issuer = new TokenService("another-secret-that-is-long-enough-123456", 60);
        TokenService verifier = new TokenService(SECRET, 60);

        assertNull(verifier.validateToken(issuer.generateToken(user)));
    }

    @Test
    void tamperedPayloadIsRejected() {
        TokenService service = new TokenService(SECRET, 60);
        String[] mine = service.generateToken(user).split("\\.");
        String[] other = service.generateToken(new User("bob", "hash", UserRole.USER)).split("\\.");

        String forged = mine[0] + "." + other[1] + "." + mine[2];

        assertNull(service.validateToken(forged));
    }

    @Test
    void expiredTokenIsRejected() {
        TokenService service = new TokenService(SECRET, -1);

        assertNull(service.validateToken(service.generateToken(user)));
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        TokenService service = new TokenService(SECRET, 60);

        assertNull(service.validateToken("not-a-jwt"));
        assertNull(service.validateToken(""));
    }

    @Test
    void shortSecretsFailFastAtStartup() {
        assertThrows(IllegalStateException.class, () -> new TokenService("too-short", 60));
        assertThrows(IllegalStateException.class, () -> new TokenService(null, 60));
    }

    @Test
    void exposesTheLifetimeInSeconds() {
        assertEquals(7200, new TokenService(SECRET, 120).getExpirationSeconds());
    }
}
