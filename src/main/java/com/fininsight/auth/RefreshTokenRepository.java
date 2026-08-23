package com.fininsight.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for RefreshToken entity.
 * Uses PostgreSQL row-level pessimistic locking on token hash lookup to prevent
 * concurrent refresh token rotation race conditions and false-positive reuse detection.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by its SHA-256 token hash with a pessimistic write lock (SELECT ... FOR UPDATE).
     * Serializes concurrent refresh requests on the same token.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds all active (non-revoked, non-expired) refresh tokens for a user.
     */
    List<RefreshToken> findAllByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, Instant now);

    /**
     * Revokes all active tokens for a specific user (used on password change or token reuse detection).
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Deletes all expired tokens older than the given timestamp (cleanup background job).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Deletes all tokens belonging to a user.
     */
    void deleteByUserId(UUID userId);
}
