package com.fininsight.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.analytics.dto.FinancialSummaryAggregation;
import com.fininsight.analytics.dto.MonthlyAggregation;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds aggregated, sanitized financial context for AI consumption.
 * Ensures zero PII (no userId, email, tokens, or raw transaction IDs).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialContextBuilder {

    private static final Pattern PROMPT_INJECTION_PATTERN =
            Pattern.compile("(?i)(\\bignore\\b|\\bprevious\\b|\\binstruction[s]?\\b|\\bsystem\\b|\\bprompt\\b|<[^>]*>|```|`|\")");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    public FinancialContext buildContext(UUID userId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : YearMonth.now().atDay(1);
        LocalDate endDate = to != null ? to : YearMonth.now().atEndOfMonth();
        YearMonth currentYM = YearMonth.from(endDate);

        // 1. Financial Summary
        FinancialSummaryAggregation summaryAgg = transactionRepository.getFinancialSummary(userId, startDate, endDate);
        BigDecimal totalIncome = (summaryAgg != null && summaryAgg.getTotalIncome() != null)
                ? summaryAgg.getTotalIncome() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalExpense = (summaryAgg != null && summaryAgg.getTotalExpense() != null)
                ? summaryAgg.getTotalExpense() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        Long count = summaryAgg != null ? summaryAgg.getTransactionCount() : null;
        long txCount = count != null ? count : 0L;

        // 2. Category Spending
        List<CategorySpendingAggregation> catAggs = transactionRepository.getSpendingByCategory(
                userId, startDate, endDate
        );
        List<FinancialContext.CategorySpendingItem> topCategories = buildCategoryItems(catAggs, totalExpense);

        // 3. Budget Overview
        FinancialContext.BudgetSummaryItem budgetSummary = buildBudgetSummary(userId, currentYM);

        // 4. Previous Month Comparison
        FinancialContext.MonthlyComparisonItem comparison = buildMonthlyComparison(userId, currentYM);

        // 5. Monthly Trend (6 months)
        List<FinancialContext.MonthlyTrendItem> monthlyTrends = buildMonthlyTrends(userId, currentYM);

        return new FinancialContext(
                startDate.toString(),
                endDate.toString(),
                totalIncome,
                totalExpense,
                netBalance,
                txCount,
                topCategories,
                budgetSummary,
                comparison,
                monthlyTrends
        );
    }

    private List<FinancialContext.CategorySpendingItem> buildCategoryItems(
            List<CategorySpendingAggregation> catAggs, BigDecimal totalExpense
    ) {
        if (catAggs == null || catAggs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> categoryIds = catAggs.stream()
                .map(CategorySpendingAggregation::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> nameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<FinancialContext.CategorySpendingItem> items = new ArrayList<>();
        for (CategorySpendingAggregation agg : catAggs.stream().limit(5).toList()) {
            String rawName = nameMap.getOrDefault(agg.getCategoryId(), "General");
            String sanitizedName = sanitizeCategoryName(rawName);
            BigDecimal amount = agg.getTotalAmount() != null ? agg.getTotalAmount() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            BigDecimal percentage;
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.divide(totalExpense, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            items.add(new FinancialContext.CategorySpendingItem(sanitizedName, amount, percentage));
        }
        return items;
    }

    private FinancialContext.BudgetSummaryItem buildBudgetSummary(UUID userId, YearMonth ym) {
        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(userId, (short) ym.getYear(), (short) ym.getMonthValue());
        if (budgets == null || budgets.isEmpty()) {
            return new FinancialContext.BudgetSummaryItem(
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0, 0, 0
            );
        }

        BigDecimal totalBudgeted = budgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        Set<UUID> categoryIds = budgets.stream().map(Budget::getCategoryId).collect(Collectors.toSet());
        List<CategorySpendingAggregation> spendings = transactionRepository.sumAmountGroupedByCategory(
                userId, categoryIds, TransactionType.EXPENSE, ym.atDay(1), ym.atEndOfMonth()
        );

        Map<UUID, BigDecimal> spendingMap = spendings.stream().collect(Collectors.toMap(
                CategorySpendingAggregation::getCategoryId,
                CategorySpendingAggregation::getTotalAmount
        ));

        BigDecimal totalSpent = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        int exceeded = 0;
        int warning = 0;

        for (Budget b : budgets) {
            BigDecimal spent = spendingMap.getOrDefault(b.getCategoryId(), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            totalSpent = totalSpent.add(spent);

            if (b.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal usage = spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP);
                if (usage.compareTo(BigDecimal.ONE) > 0) {
                    exceeded++;
                } else if (usage.compareTo(new BigDecimal("0.8000")) >= 0) {
                    warning++;
                }
            }
        }

        BigDecimal usagePercentage = (totalBudgeted.compareTo(BigDecimal.ZERO) > 0)
                ? totalSpent.divide(totalBudgeted, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new FinancialContext.BudgetSummaryItem(
                totalBudgeted, totalSpent, usagePercentage, budgets.size(), exceeded, warning
        );
    }

    private FinancialContext.MonthlyComparisonItem buildMonthlyComparison(UUID userId, YearMonth currentYM) {
        YearMonth previousYM = currentYM.minusMonths(1);

        FinancialSummaryAggregation curr = transactionRepository.getFinancialSummary(userId, currentYM.atDay(1), currentYM.atEndOfMonth());
        FinancialSummaryAggregation prev = transactionRepository.getFinancialSummary(userId, previousYM.atDay(1), previousYM.atEndOfMonth());

        BigDecimal currExp = (curr != null && curr.getTotalExpense() != null) ? curr.getTotalExpense() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal prevExp = (prev != null && prev.getTotalExpense() != null) ? prev.getTotalExpense() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal diff = currExp.subtract(prevExp);
        BigDecimal changePercentage;
        if (prevExp.compareTo(BigDecimal.ZERO) > 0) {
            changePercentage = diff.abs().divide(prevExp, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        } else {
            changePercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String trend = diff.compareTo(BigDecimal.ZERO) > 0 ? "INCREASED" : (diff.compareTo(BigDecimal.ZERO) < 0 ? "DECREASED" : "UNCHANGED");

        return new FinancialContext.MonthlyComparisonItem(currExp, prevExp, diff, changePercentage, trend);
    }

    private List<FinancialContext.MonthlyTrendItem> buildMonthlyTrends(UUID userId, YearMonth currentYM) {
        YearMonth startYM = currentYM.minusMonths(5);
        List<MonthlyAggregation> aggs = transactionRepository.getMonthlySummary(userId, startYM.atDay(1), currentYM.atEndOfMonth());

        Map<String, MonthlyAggregation> map = new HashMap<>();
        if (aggs != null) {
            for (MonthlyAggregation a : aggs) {
                if (a.getYear() != null && a.getMonth() != null) {
                    map.put(String.format("%04d-%02d", a.getYear(), a.getMonth()), a);
                }
            }
        }

        List<FinancialContext.MonthlyTrendItem> list = new ArrayList<>();
        YearMonth iterator = startYM;
        while (!iterator.isAfter(currentYM)) {
            String key = iterator.toString();
            MonthlyAggregation a = map.get(key);
            BigDecimal inc = (a != null && a.getTotalIncome() != null) ? a.getTotalIncome() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            BigDecimal exp = (a != null && a.getTotalExpense() != null) ? a.getTotalExpense() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            BigDecimal savings = inc.subtract(exp);

            list.add(new FinancialContext.MonthlyTrendItem(key, inc, exp, savings));
            iterator = iterator.plusMonths(1);
        }
        return list;
    }

    /**
     * Sanitizes user-defined category name to prevent prompt injection and limits character length.
     */
    public String sanitizeCategoryName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Uncategorized";
        }
        String clean = PROMPT_INJECTION_PATTERN.matcher(raw.trim()).replaceAll(" ");
        clean = clean.replaceAll("\\s+", " ").trim();
        if (clean.isBlank()) {
            clean = "Custom Category";
        }
        return clean.length() > 50 ? clean.substring(0, 50) : clean;
    }
}
