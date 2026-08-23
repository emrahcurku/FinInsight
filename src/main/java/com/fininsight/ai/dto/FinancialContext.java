package com.fininsight.ai.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated, sanitized financial context passed to AI provider.
 * Contains ZERO PII (no userId, email, token, or individual transaction IDs).
 */
public record FinancialContext(
        String periodFrom,
        String periodTo,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        long transactionCount,
        List<CategorySpendingItem> topCategories,
        BudgetSummaryItem budgetSummary,
        MonthlyComparisonItem previousMonthComparison,
        List<MonthlyTrendItem> monthlyTrends
) {
    public FinancialContext {
        topCategories = topCategories != null ? Collections.unmodifiableList(topCategories) : Collections.emptyList();
        monthlyTrends = monthlyTrends != null ? Collections.unmodifiableList(monthlyTrends) : Collections.emptyList();
    }

    public record CategorySpendingItem(
            String categoryName,
            BigDecimal amount,
            BigDecimal percentage
    ) {}

    public record BudgetSummaryItem(
            BigDecimal totalBudgeted,
            BigDecimal totalSpent,
            BigDecimal usagePercentage,
            int activeBudgetsCount,
            int exceededBudgetsCount,
            int warningBudgetsCount
    ) {}

    public record MonthlyComparisonItem(
            BigDecimal currentMonthExpense,
            BigDecimal previousMonthExpense,
            BigDecimal expenseChangeAmount,
            BigDecimal expenseChangePercentage,
            String trendDirection
    ) {}

    public record MonthlyTrendItem(
            String yearMonth,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal netSavings
    ) {}
}
