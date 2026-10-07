package com.careersphere.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final SecretKey key;
    private final long ttlHours;

    public JwtTokenService(
        @Value("${app.security.jwt-secret}") String secret,
        @Value("${app.security.jwt-ttl-hours:12}") long ttlHours
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Set JWT_SECRET (or SESSION_SECRET) before starting CareerSphere.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = Keys.hmacShaKeyFor(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signing configuration is invalid.");
        }
        this.ttlHours = Math.max(1, ttlHours);
    }

    public String issue(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlHours * 3600)))
            .signWith(key)
            .compact();
    }

    public String getSubject(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public long getTtlSeconds() { return ttlHours * 3600; }
}
