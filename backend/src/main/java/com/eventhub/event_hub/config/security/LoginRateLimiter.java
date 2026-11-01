package com.eventhub.event_hub.config.security;

import com.eventhub.event_hub.exceptions.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Brute-force protection: after N failed logins for the same login+IP inside the time window,
 * further attempts are rejected (429) until the oldest failure leaves the window.
 * In-memory on purpose (single instance); a multi-instance deployment would back this with Redis.
 */
@Component
public class LoginRateLimiter {

    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${app.security.login-max-failures:5}") int maxFailures,
                            @Value("${app.security.login-window-minutes:15}") long windowMinutes,
                            Clock clock) {
        this.maxFailures = maxFailures;
        this.window = Duration.ofMinutes(windowMinutes);
        this.clock = clock;
    }

    public void assertNotBlocked(String key) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return;
        }
        synchronized (attempts) {
            Instant now = clock.instant();
            prune(attempts, now);
            if (attempts.size() >= maxFailures) {
                long retryAfter = Math.max(1, Duration.between(now, attempts.peekFirst().plus(window)).getSeconds());
                throw new TooManyRequestsException("Too many failed login attempts. Try again later.", retryAfter);
            }
        }
    }

    public void recordFailure(String key) {
        if (failures.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            Instant now = clock.instant();
            prune(attempts, now);
            attempts.addLast(now);
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private void prune(Deque<Instant> attempts, Instant now) {
        Instant cutoff = now.minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
