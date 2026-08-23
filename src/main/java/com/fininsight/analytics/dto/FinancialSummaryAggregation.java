package com.fininsight.analytics.dto;

import java.math.BigDecimal;

/**
 * Projection interface for database-level financial summary aggregations.
 */
public interface FinancialSummaryAggregation {
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
    Long getTransactionCount();
}
