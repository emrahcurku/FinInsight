package com.fininsight.ai;

import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.ai.service.DeterministicInsightEngine;
import com.fininsight.dashboard.dto.InsightSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DeterministicInsightEngineTest {

    private final DeterministicInsightEngine engine = new DeterministicInsightEngine();

    @Test
    @DisplayName("Should generate no activity insight when transaction count is zero")
    public void shouldGenerateNoActivityInsight() {
        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                List.of(), null, null, List.of()
        );

        AiGenerationResult result = engine.generateFallbackInsights(context);

        assertThat(result.model()).isEqualTo("deterministic-rule-engine");
        assertThat(result.insights()).hasSize(1);
        assertThat(result.insights().get(0).type()).isEqualTo("NO_ACTIVITY");
    }

    @Test
    @DisplayName("Should generate deficit and budget overrun insights")
    public void shouldGenerateDeficitAndBudgetOverrun() {
        FinancialContext.BudgetSummaryItem budget = new FinancialContext.BudgetSummaryItem(
                new BigDecimal("1000"), new BigDecimal("1300"), new BigDecimal("130"), 1, 1, 0
        );

        FinancialContext.CategorySpendingItem topCat = new FinancialContext.CategorySpendingItem(
                "Dining", new BigDecimal("800"), new BigDecimal("61.5")
        );

        FinancialContext.MonthlyComparisonItem comp = new FinancialContext.MonthlyComparisonItem(
                new BigDecimal("1300"), new BigDecimal("1000"), new BigDecimal("300"), new BigDecimal("30"), "INCREASED"
        );

        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                new BigDecimal("1000.00"), new BigDecimal("1300.00"), new BigDecimal("-300.00"), 15L,
                List.of(topCat), budget, comp, List.of()
        );

        AiGenerationResult result = engine.generateFallbackInsights(context);

        assertThat(result.insights()).anyMatch(i -> i.type().equals("DEFICIT_WARNING") && i.severity() == InsightSeverity.WARNING);
        assertThat(result.insights()).anyMatch(i -> i.type().equals("BUDGET_OVERRUN") && i.severity() == InsightSeverity.DANGER);
        assertThat(result.insights()).anyMatch(i -> i.type().equals("TOP_EXPENSE_CATEGORY"));
        assertThat(result.insights()).anyMatch(i -> i.type().equals("SPENDING_INCREASE"));
        assertThat(result.recommendations()).isNotEmpty();
    }
}
