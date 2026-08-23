package com.fininsight.auth;

import com.fininsight.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Utility component for JWT access token generation, validation,
 * claims extraction, and secure random refresh token generation and hashing.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProvider(
            @Value("${application.security.jwt.secret-key:}") String secretKeyString,
            @Value("${application.security.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${application.security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        if (secretKeyString == null || secretKeyString.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT secret key must be configured via 'application.security.jwt.secret-key' or JWT_SECRET environment variable."
            );
        }
        byte[] keyBytes = secretKeyString.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 characters) long.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generates a signed stateless JWT access token.
     */
    public String generateAccessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the User ID (subject) from a validated token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email claim from a validated token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the role claim from a validated token.
     */
    public Role getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String roleStr = claims.get("role", String.class);
        return Role.valueOf(roleStr);
    }

    /**
     * Validates JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException | JwtException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Generates a cryptographically secure, random 256-bit refresh token encoded as Base64URL.
     */
    public String generateRawRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Calculates the SHA-256 hex hash of a raw refresh token for database persistence.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
