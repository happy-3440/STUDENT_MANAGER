package com.studentmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies short-lived JWT ACCESS tokens only. Refresh tokens are
 * opaque random strings stored (hashed) in refresh_tokens - see
 * RefreshTokenRepository - because a refresh token must be revocable, and a
 * self-contained JWT cannot be revoked before it expires.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessMinutes
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) is not set. Refusing to start with no signing key. " +
                    "Generate one with: openssl rand -base64 48"
            );
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTtl = Duration.ofMinutes(accessMinutes);
    }

    public String issueAccessToken(UUID userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * Parses and verifies the token. Throws JwtException (expired, bad
     * signature, malformed) if it's not valid - callers must not swallow
     * this silently.
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }
}
