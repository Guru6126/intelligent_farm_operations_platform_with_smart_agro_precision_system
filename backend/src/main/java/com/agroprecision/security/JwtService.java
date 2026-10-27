package com.agroprecision.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutes) {
        if (secret.length() < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = minutes * 60_000L;
    }

    public String issue(String userId) {
        Date now = new Date();
        return Jwts.builder().subject(userId).issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
    }

    /** Returns the user id, or null if the token is invalid/expired. */
    public String verify(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
