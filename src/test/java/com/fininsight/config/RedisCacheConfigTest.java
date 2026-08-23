package com.fininsight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.analytics.dto.FinancialSummaryResponse;
import com.fininsight.dashboard.dto.BudgetOverviewDTO;
import com.fininsight.dashboard.dto.DashboardResponse;
import com.fininsight.dashboard.dto.FinancialInsightDTO;
import com.fininsight.dashboard.dto.FinancialSummaryDTO;
import com.fininsight.dashboard.dto.InsightSeverity;
import com.fininsight.dashboard.dto.PreviousMonthComparisonDTO;
import com.fininsight.dashboard.dto.TrendDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisCacheConfigTest {

    @Test
    @DisplayName("Cache ObjectMapper serializes and deserializes FinancialSummaryResponse preserving BigDecimal and dates")
    void testFinancialSummarySerialization() throws Exception {
        ObjectMapper mapper = RedisCacheConfig.createCacheObjectMapper();

        FinancialSummaryResponse original = new FinancialSummaryResponse(
                new BigDecimal("15000.5000"),
                new BigDecimal("4200.2500"),
                new BigDecimal("10800.2500"),
                15,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        String json = mapper.writeValueAsString(original);
        assertThat(json).contains("totalIncome");
        assertThat(json).contains("15000.5000");

        FinancialSummaryResponse deserialized = mapper.readValue(json, FinancialSummaryResponse.class);
        assertThat(deserialized.totalIncome()).isEqualByComparingTo("15000.5000");
        assertThat(deserialized.totalExpense()).isEqualByComparingTo("4200.2500");
        assertThat(deserialized.netBalance()).isEqualByComparingTo("10800.2500");
        assertThat(deserialized.transactionCount()).isEqualTo(15);
        assertThat(deserialized.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(deserialized.to()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("Cache ObjectMapper serializes and deserializes composite DashboardResponse")
    void testDashboardResponseSerialization() throws Exception {
        ObjectMapper mapper = RedisCacheConfig.createCacheObjectMapper();

        FinancialSummaryDTO summary = new FinancialSummaryDTO(
                new BigDecimal("8000.0000"),
                new BigDecimal("3000.0000"),
                new BigDecimal("5000.0000"),
                8,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        BudgetOverviewDTO budgetOverview = new BudgetOverviewDTO(
                new BigDecimal("5000.0000"),
                new BigDecimal("3000.0000"),
                new BigDecimal("2000.0000"),
                new BigDecimal("60.000"),
                1,
                0,
                0
        );

        PreviousMonthComparisonDTO comparison = new PreviousMonthComparisonDTO(
                new BigDecimal("3000.0000"),
                new BigDecimal("2500.0000"),
                new BigDecimal("500.0000"),
                new BigDecimal("20.000"),
                TrendDirection.INCREASED
        );

        FinancialInsightDTO insight = new FinancialInsightDTO(
                "TOP_SPENDING_CATEGORY",
                "Top Expense Category",
                "Groceries is your highest expense",
                InsightSeverity.INFO
        );

        DashboardResponse response = new DashboardResponse(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                budgetOverview,
                Collections.emptyList(),
                comparison,
                List.of(insight)
        );

        String json = mapper.writeValueAsString(response);
        DashboardResponse deserialized = mapper.readValue(json, DashboardResponse.class);

        assertThat(deserialized.financialSummary().totalIncome()).isEqualByComparingTo("8000.0000");
        assertThat(deserialized.previousMonthComparison().trend()).isEqualTo(TrendDirection.INCREASED);
        assertThat(deserialized.insights()).hasSize(1);
        assertThat(deserialized.insights().get(0).severity()).isEqualTo(InsightSeverity.INFO);
    }
}
