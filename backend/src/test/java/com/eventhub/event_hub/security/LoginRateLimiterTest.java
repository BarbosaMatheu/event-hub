package com.eventhub.event_hub.security;

import com.eventhub.event_hub.config.security.LoginRateLimiter;
import com.eventhub.event_hub.exceptions.TooManyRequestsException;
import com.eventhub.event_hub.utils.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    private MutableClock clock;
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2030-01-01T00:00:00Z"));
        limiter = new LoginRateLimiter(3, 15, clock);
    }

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(key);
        }
    }

    @Test
    void allowsAttemptsBelowTheLimit() {
        fail("ann|1.1.1.1", 2);

        assertDoesNotThrow(() -> limiter.assertNotBlocked("ann|1.1.1.1"));
    }

    @Test
    void blocksAfterTooManyFailuresAndReportsRetryAfter() {
        fail("ann|1.1.1.1", 3);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> limiter.assertNotBlocked("ann|1.1.1.1"));

        assertTrue(ex.getRetryAfterSeconds() > 0);
        assertTrue(ex.getRetryAfterSeconds() <= 15 * 60);
    }

    @Test
    void unblocksOnceTheFailuresLeaveTheWindow() {
        fail("ann|1.1.1.1", 3);

        clock.advance(Duration.ofMinutes(16));

        assertDoesNotThrow(() -> limiter.assertNotBlocked("ann|1.1.1.1"));
    }

    @Test
    void successfulLoginClearsTheCounter() {
        fail("ann|1.1.1.1", 3);

        limiter.reset("ann|1.1.1.1");

        assertDoesNotThrow(() -> limiter.assertNotBlocked("ann|1.1.1.1"));
    }

    @Test
    void differentKeysDoNotAffectEachOther() {
        fail("ann|1.1.1.1", 3);

        assertDoesNotThrow(() -> limiter.assertNotBlocked("bob|1.1.1.1"));
        assertDoesNotThrow(() -> limiter.assertNotBlocked("ann|2.2.2.2"));
    }
}
