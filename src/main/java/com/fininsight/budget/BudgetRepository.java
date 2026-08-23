package com.fininsight.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Budget entity.
 * Supports specification-based dynamic filtering, pagination, and ownership verification.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID>, JpaSpecificationExecutor<Budget> {

    /**
     * Finds a single budget by ID and user ID for security boundary check.
     */
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds a budget for a specific user, category, and month/year period.
     */
    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(
            UUID userId, UUID categoryId, short year, short month);

    /**
     * Finds all budgets of a user for a specific month and year.
     */
    List<Budget> findByUserIdAndYearAndMonth(UUID userId, short year, short month);

    /**
     * Checks if a budget already exists for a user, category, and month/year period.
     */
    boolean existsByUserIdAndCategoryIdAndYearAndMonth(
            UUID userId, UUID categoryId, short year, short month);

    /**
     * Checks if any budget references the given category ID.
     */
    boolean existsByCategoryId(UUID categoryId);
}
