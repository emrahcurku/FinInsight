package com.fininsight.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection interface for aggregated category spending.
 */
public interface CategorySpendingAggregation {
    UUID getCategoryId();
    BigDecimal getTotalAmount();
}
