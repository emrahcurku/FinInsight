package com.fininsight.analytics.dto;

import java.math.BigDecimal;

/**
 * Projection interface for database-level monthly financial aggregations.
 */
public interface MonthlyAggregation {
    Integer getYear();
    Integer getMonth();
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
}
