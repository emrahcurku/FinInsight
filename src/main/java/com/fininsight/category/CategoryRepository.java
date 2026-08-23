package com.fininsight.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Category entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Finds all system default categories (where userId is null).
     */
    List<Category> findByUserIdIsNull();

    /**
     * Finds all custom categories created by a specific user.
     */
    List<Category> findByUserId(UUID userId);

    /**
     * Finds a single category by ID and user ID for custom category operations.
     */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds all categories accessible to a user (both system categories and the user's custom categories)
     * with deterministic alphabetical and ID sorting.
     */
    @Query("SELECT c FROM Category c WHERE c.userId IS NULL OR c.userId = :userId ORDER BY c.name ASC, c.id ASC")
    List<Category> findAllAvailableForUser(@Param("userId") UUID userId);

    /**
     * Finds categories by type accessible to a user.
     */
    @Query("SELECT c FROM Category c WHERE (c.userId IS NULL OR c.userId = :userId) AND c.type = :type ORDER BY c.name ASC, c.id ASC")
    List<Category> findAvailableForUserByType(@Param("userId") UUID userId, @Param("type") CategoryType type);

    /**
     * Checks if a category with the same name and type already exists for a user.
     */
    boolean existsByUserIdAndNameIgnoreCaseAndType(UUID userId, String name, CategoryType type);

    /**
     * Checks if a system category with the same name and type already exists.
     */
    boolean existsByUserIdIsNullAndNameIgnoreCaseAndType(String name, CategoryType type);
}
