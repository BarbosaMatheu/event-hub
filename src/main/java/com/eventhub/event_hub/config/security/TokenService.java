package com.eventhub.event_hub.config.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.eventhub.event_hub.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import java.util.Date;

@Service
public class TokenService {

    private static final long EXPIRATION_MILLIS = 86_400_000; // 24 hours

    private final SecretKey key;

    // No insecure default here on purpose: the app must fail fast at startup
    // if JWT_SECRET isn't configured, instead of silently signing tokens
    // with a well-known, hardcoded fallback secret.
    public TokenService(@Value("${api.security.token.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MILLIS))
                .signWith(key)
                .compact();
    }

    public String validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
}
