package com.eventhub.event_hub.config.security;

import com.eventhub.event_hub.models.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    // No default for the secret on purpose: the app must fail fast at startup if JWT_SECRET is
    // missing or weak, instead of silently signing tokens with a well-known value.
    public TokenService(@Value("${api.security.token.secret}") String secret,
                        @Value("${api.security.token.expiration-minutes:1440}") long expirationMinutes) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    /** Returns the token's subject (login), or null if the token is invalid, tampered with or expired. */
    public String validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60L;
    }
}
