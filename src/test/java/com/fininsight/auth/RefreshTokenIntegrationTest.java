package com.fininsight.auth;

import com.fininsight.user.User;
import com.fininsight.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RefreshTokenIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Create, persist and find refresh token by hash")
    void testCreateAndFindByTokenHash() {
        User user = userRepository.saveAndFlush(
                new User("auth.user@example.com", "hashed_pwd", "Auth", "User"));

        String tokenHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshToken token = new RefreshToken(user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.saveAndFlush(token);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isActive()).isTrue();
        assertThat(token.isExpired()).isFalse();

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(tokenHash);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Unique token hash constraint enforcement")
    void testUniqueTokenHashConstraint() {
        User user = userRepository.saveAndFlush(
                new User("auth.unique@example.com", "hashed_pwd", "Unique", "User"));

        String tokenHash = "duplicate_hash_value_12345678901234567890123456789012345678901234";
        RefreshToken token1 = new RefreshToken(user.getId(), tokenHash, Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(token1);

        RefreshToken token2 = new RefreshToken(user.getId(), tokenHash, Instant.now().plus(7, ChronoUnit.DAYS));
        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(token2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Query active tokens for user")
    void testFindActiveTokensForUser() {
        User user = userRepository.saveAndFlush(
                new User("auth.active@example.com", "hashed_pwd", "Active", "User"));

        // 1. Active token
        RefreshToken activeToken = new RefreshToken(
                user.getId(), "hash_active_1", Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(activeToken);

        // 2. Revoked token
        RefreshToken revokedToken = new RefreshToken(
                user.getId(), "hash_revoked_2", Instant.now().plus(7, ChronoUnit.DAYS));
        revokedToken.revoke();
        refreshTokenRepository.saveAndFlush(revokedToken);

        // 3. Expired token
        RefreshToken expiredToken = new RefreshToken(
                user.getId(), "hash_expired_3", Instant.now().minus(1, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(expiredToken);

        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUserIdAndRevokedFalseAndExpiresAtAfter(user.getId(), Instant.now());

        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.getFirst().getTokenHash()).isEqualTo("hash_active_1");
    }

    @Test
    @DisplayName("Revoke all tokens for user on password change")
    void testRevokeAllByUserId() {
        User user = userRepository.saveAndFlush(
                new User("auth.revoke@example.com", "hashed_pwd", "Revoke", "User"));

        RefreshToken token1 = new RefreshToken(user.getId(), "hash_t1", Instant.now().plus(7, ChronoUnit.DAYS));
        RefreshToken token2 = new RefreshToken(user.getId(), "hash_t2", Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(token1);
        refreshTokenRepository.saveAndFlush(token2);

        int updatedCount = refreshTokenRepository.revokeAllByUserId(user.getId());
        assertThat(updatedCount).isEqualTo(2);

        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUserIdAndRevokedFalseAndExpiresAtAfter(user.getId(), Instant.now());
        assertThat(activeTokens).isEmpty();
    }
}
