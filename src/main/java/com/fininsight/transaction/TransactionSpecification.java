package com.fininsight.transaction;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specification builder for dynamic transaction query filtering.
 * Enforces strict user isolation on every query.
 */
public final class TransactionSpecification {

    private TransactionSpecification() {
        // Utility class
    }

    /**
     * Builds a composite JPA Specification matching user ownership and optional filter criteria.
     *
     * @param userId      mandatory authenticated user ID
     * @param from        optional start date (inclusive)
     * @param to          optional end date (inclusive)
     * @param type        optional transaction type (INCOME/EXPENSE)
     * @param categoryId  optional category ID filter
     * @return JPA Specification for Transaction entity
     */
    public static Specification<Transaction> withFilters(
            UUID userId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            UUID categoryId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory user ownership filter (enforces user boundary)
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            // 2. Optional date range filters
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), to));
            }

            // 3. Optional transaction type filter
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // 4. Optional category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
