package com.fininsight.budget;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Specification builder for dynamic multi-criteria budget queries.
 * Enforces authenticated user data isolation on all queries.
 */
public final class BudgetSpecification {

    private BudgetSpecification() {
        // Utility class
    }

    /**
     * Builds a combined dynamic specification enforcing userId boundary and optional filters.
     *
     * @param userId     Mandatory user UUID for isolation
     * @param year       Optional budget year filter
     * @param month      Optional budget month filter
     * @param categoryId Optional category UUID filter
     * @return Specification to execute via JpaSpecificationExecutor
     */
    public static Specification<Budget> withFilters(
            UUID userId,
            Integer year,
            Integer month,
            UUID categoryId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory user boundary (prevents data leakage / IDOR)
            predicates.add(cb.equal(root.get("userId"), userId));

            // 2. Optional year filter
            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year.shortValue()));
            }

            // 3. Optional month filter
            if (month != null) {
                predicates.add(cb.equal(root.get("month"), month.shortValue()));
            }

            // 4. Optional category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
