package com.fininsight.auth;

import com.fininsight.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLXRlc3Qtand0LXNlY3JldC1rZXktbWluLTY0LWNoYXJhY3RlcnMtbG9uZw==";
    private final long accessExpirationMs = 60000; // 1 minute
    private final long refreshExpirationMs = 604800000; // 7 days

    @BeforeEach
    public void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, accessExpirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Generate, validate and parse valid JWT access token")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String email = "jwt.test@example.com";
        Role role = Role.ROLE_USER;

        String token = jwtTokenProvider.generateAccessToken(userId, email, role);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
    }

    @Test
    @DisplayName("Invalid signature or tampered token should fail validation")
    void testTamperedTokenFailsValidation() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com", Role.ROLE_USER);

        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void testExpiredToken() {
        // Provider with -1ms expiration (immediately expired)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secretKey, -1000, refreshExpirationMs);
        UUID userId = UUID.randomUUID();

        String token = expiredProvider.generateAccessToken(userId, "expired@example.com", Role.ROLE_USER);

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Generate CSPRNG random refresh tokens and SHA-256 hash consistency")
    void testRefreshTokenGenerationAndHashing() {
        String token1 = jwtTokenProvider.generateRawRefreshToken();
        String token2 = jwtTokenProvider.generateRawRefreshToken();

        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);

        String hash1 = jwtTokenProvider.hashToken(token1);
        String hash1Repeat = jwtTokenProvider.hashToken(token1);
        String hash2 = jwtTokenProvider.hashToken(token2);

        assertThat(hash1).hasSize(64); // SHA-256 hex is 64 chars
        assertThat(hash1).isEqualTo(hash1Repeat);
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Unconfigured or blank JWT secret must fail fast on startup with IllegalStateException")
    void testMissingSecretKeyFailsStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider("", accessExpirationMs, refreshExpirationMs))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");

        assertThatThrownBy(() -> new JwtTokenProvider(null, accessExpirationMs, refreshExpirationMs))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");
    }

    @Test
    @DisplayName("Weak or short (< 32 bytes) JWT secret must fail fast on startup with IllegalArgumentException")
    void testShortSecretKeyFailsStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider("short-secret-key", accessExpirationMs, refreshExpirationMs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 256 bits");
    }
}
