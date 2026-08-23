package com.fininsight.transaction;

import com.fininsight.transaction.dto.CategorySpendingAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Transaction entity.
 * Supports specification-based dynamic filtering, pagination, ownership verification,
 * and batch spending aggregations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Finds a single transaction by ID and user ID for security boundary check.
     */
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds all transactions for a user with pagination and sorting.
     */
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    /**
     * Finds all transactions for a user within a specific date range.
     */
    Page<Transaction> findByUserIdAndTransactionDateBetween(
            UUID userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Finds all transactions for a user by category.
     */
    Page<Transaction> findByUserIdAndCategoryId(UUID userId, UUID categoryId, Pageable pageable);

    /**
     * Finds all transactions for a user by type (INCOME/EXPENSE).
     */
    Page<Transaction> findByUserIdAndType(UUID userId, TransactionType type, Pageable pageable);

    /**
     * Checks if any transaction references the given category ID.
     */
    boolean existsByCategoryId(UUID categoryId);

    /**
     * Calculates the sum of transaction amounts for a user, category, type, and date range.
     * Used for budget spending calculation.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.categoryId = :categoryId " +
           "AND t.type = :type " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregates total transaction amount grouped by category for a batch of category IDs.
     * Prevents N+1 queries when calculating spending across a paginated list of budgets.
     */
    @Query("SELECT t.categoryId AS categoryId, COALESCE(SUM(t.amount), 0) AS totalAmount " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.categoryId IN :categoryIds " +
           "AND t.type = :type " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY t.categoryId")
    List<CategorySpendingAggregation> sumAmountGroupedByCategory(
            @Param("userId") UUID userId,
            @Param("categoryIds") Collection<UUID> categoryIds,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Database-level aggregation for total income, total expense, and transaction count.
     */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN t.type = com.fininsight.transaction.TransactionType.INCOME THEN t.amount ELSE 0 END), 0) AS totalIncome, " +
           "COALESCE(SUM(CASE WHEN t.type = com.fininsight.transaction.TransactionType.EXPENSE THEN t.amount ELSE 0 END), 0) AS totalExpense, " +
           "COUNT(t) AS transactionCount " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate")
    com.fininsight.analytics.dto.FinancialSummaryAggregation getFinancialSummary(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregates spending for all expense categories ordered by total amount descending.
     */
    @Query("SELECT t.categoryId AS categoryId, COALESCE(SUM(t.amount), 0) AS totalAmount " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.type = com.fininsight.transaction.TransactionType.EXPENSE " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY t.categoryId " +
           "ORDER BY COALESCE(SUM(t.amount), 0) DESC, t.categoryId ASC")
    List<CategorySpendingAggregation> getSpendingByCategory(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Groups transaction income and expense by year and month.
     */
    @Query("SELECT " +
           "YEAR(t.transactionDate) AS year, " +
           "MONTH(t.transactionDate) AS month, " +
           "COALESCE(SUM(CASE WHEN t.type = com.fininsight.transaction.TransactionType.INCOME THEN t.amount ELSE 0 END), 0) AS totalIncome, " +
           "COALESCE(SUM(CASE WHEN t.type = com.fininsight.transaction.TransactionType.EXPENSE THEN t.amount ELSE 0 END), 0) AS totalExpense " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
           "ORDER BY YEAR(t.transactionDate) DESC, MONTH(t.transactionDate) DESC")
    List<com.fininsight.analytics.dto.MonthlyAggregation> getMonthlySummary(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
