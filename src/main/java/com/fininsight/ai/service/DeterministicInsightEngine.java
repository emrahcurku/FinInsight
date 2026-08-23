package com.fininsight.ai.service;

import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.AiInsightItem;
import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.dashboard.dto.InsightSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule-based fallback engine when AI provider is unavailable or disabled.
 * Produces structured, educational financial recommendations.
 */
@Component
public class DeterministicInsightEngine {

    public AiGenerationResult generateFallbackInsights(FinancialContext context) {
        List<AiInsightItem> insights = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Activity Check
        if (context.transactionCount() == 0) {
            insights.add(new AiInsightItem(
                    "NO_ACTIVITY",
                    InsightSeverity.INFO,
                    "No Financial Activity",
                    "No transactions were recorded for the selected period.",
                    "Start logging your daily income and expenses to unlock comprehensive financial analytics."
            ));
            recommendations.add("Record your recurring expenses to build a foundational financial profile.");
            return new AiGenerationResult(
                    "No transactions recorded for the selected period.",
                    insights,
                    recommendations,
                    "deterministic-rule-engine"
            );
        }

        // 2. Net Balance / Savings Posture
        if (context.netBalance().compareTo(BigDecimal.ZERO) < 0 && context.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deficit = context.netBalance().abs().setScale(2, RoundingMode.HALF_UP);
            insights.add(new AiInsightItem(
                    "DEFICIT_WARNING",
                    InsightSeverity.WARNING,
                    "Expenses Exceed Income",
                    "Your total expenses exceed your income by $" + deficit + " in this period.",
                    "Review discretionary spending and pause non-essential purchases to restore a positive balance."
            ));
            recommendations.add("Identify non-essential recurring subscriptions and consider temporary reductions.");
        } else if (context.netBalance().compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new AiInsightItem(
                    "POSITIVE_SAVINGS",
                    InsightSeverity.INFO,
                    "Positive Net Balance",
                    "You maintained a positive net savings balance of $" + context.netBalance().setScale(2, RoundingMode.HALF_UP) + ".",
                    "Consider allocating a portion of surplus savings to an emergency fund or planned goals."
            ));
            recommendations.add("Build or maintain a 3-6 month emergency fund with surplus cash flow.");
        }

        // 3. Budget Status
        if (context.budgetSummary() != null) {
            if (context.budgetSummary().exceededBudgetsCount() > 0) {
                insights.add(new AiInsightItem(
                        "BUDGET_OVERRUN",
                        InsightSeverity.DANGER,
                        "Budget Limits Exceeded",
                        context.budgetSummary().exceededBudgetsCount() + " category budget(s) have exceeded their allocated monthly limit.",
                        "Reallocate funds from under-utilized categories or adjust next month's spending limits."
                ));
                recommendations.add("Set up real-time spending tracking for categories that frequently exceed budget limits.");
            } else if (context.budgetSummary().warningBudgetsCount() > 0) {
                insights.add(new AiInsightItem(
                        "BUDGET_THRESHOLD_WARNING",
                        InsightSeverity.WARNING,
                        "Budget Approaching Limit",
                        context.budgetSummary().warningBudgetsCount() + " category budget(s) have reached over 80% of their limit.",
                        "Slow down spending in threshold categories for the remainder of the billing period."
                ));
            }
        }

        // 4. Top Expense Category
        if (context.topCategories() != null && !context.topCategories().isEmpty()) {
            FinancialContext.CategorySpendingItem top = context.topCategories().get(0);
            if (top.amount().compareTo(BigDecimal.ZERO) > 0) {
                insights.add(new AiInsightItem(
                        "TOP_EXPENSE_CATEGORY",
                        InsightSeverity.INFO,
                        "Highest Spending Category: " + top.categoryName(),
                        "Your highest expenditure was in " + top.categoryName() + " ($" + top.amount().setScale(2, RoundingMode.HALF_UP) + "), accounting for " + top.percentage().setScale(1, RoundingMode.HALF_UP) + "% of total expenses.",
                        "Explore category-specific savings opportunities or establish a dedicated budget limit."
                ));
                recommendations.add("Audit " + top.categoryName() + " expenses to identify potential recurring cost-saving optimizations.");
            }
        }

        // 5. Monthly Comparison Trend
        if (context.previousMonthComparison() != null && context.previousMonthComparison().previousMonthExpense().compareTo(BigDecimal.ZERO) > 0) {
            FinancialContext.MonthlyComparisonItem comp = context.previousMonthComparison();
            if ("INCREASED".equalsIgnoreCase(comp.trendDirection())) {
                insights.add(new AiInsightItem(
                        "SPENDING_INCREASE",
                        InsightSeverity.WARNING,
                        "Monthly Spending Increased",
                        "Your expenses increased by " + comp.expenseChangePercentage().setScale(1, RoundingMode.HALF_UP) + "% compared to the prior month.",
                        "Compare this month's variable expenses with last month to identify what drove the increase."
                ));
            } else if ("DECREASED".equalsIgnoreCase(comp.trendDirection())) {
                insights.add(new AiInsightItem(
                        "SPENDING_DECREASE",
                        InsightSeverity.INFO,
                        "Monthly Spending Reduced",
                        "Great job! Your expenses decreased by " + comp.expenseChangePercentage().setScale(1, RoundingMode.HALF_UP) + "% compared to the prior month.",
                        "Maintain this spending discipline and continue steering savings toward long-term goals."
                ));
            }
        }

        String summary = String.format(
                "Financial review for %s to %s: Total income $%s, total expense $%s, with a net balance of $%s.",
                context.periodFrom(),
                context.periodTo(),
                context.totalIncome().setScale(2, RoundingMode.HALF_UP),
                context.totalExpense().setScale(2, RoundingMode.HALF_UP),
                context.netBalance().setScale(2, RoundingMode.HALF_UP)
        );

        if (recommendations.isEmpty()) {
            recommendations.add("Review monthly spending breakdowns regularly to align with your personal financial objectives.");
        }

        return new AiGenerationResult(summary, insights, recommendations, "deterministic-rule-engine");
    }
}
