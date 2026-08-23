package com.fininsight.dashboard;

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
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fininsight.analytics.dto.FinancialSummaryAggregation;
import com.fininsight.analytics.dto.MonthlyAggregation;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.budget.ThresholdStatus;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.config.CacheNames;
import com.fininsight.dashboard.dto.BudgetOverviewDTO;
import com.fininsight.dashboard.dto.CategorySpendingDTO;
import com.fininsight.dashboard.dto.DashboardResponse;
import com.fininsight.dashboard.dto.FinancialInsightDTO;
import com.fininsight.dashboard.dto.FinancialSummaryDTO;
import com.fininsight.dashboard.dto.InsightSeverity;
import com.fininsight.dashboard.dto.MonthlyTrendDTO;
import com.fininsight.dashboard.dto.PreviousMonthComparisonDTO;
import com.fininsight.dashboard.dto.RecentTransactionDTO;
import com.fininsight.dashboard.dto.TrendDirection;
import com.fininsight.transaction.Transaction;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service orchestrating composite dashboard aggregation and insight generation.
 * All operations are read-only, user-isolated, and leverage batch/DB-level queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    /**
     * Aggregates all dashboard sections into a single optimized payload.
     */
    @Cacheable(value = CacheNames.DASHBOARD, key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')")
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = resolveDateRange(from, to);

        // 1. Financial Summary for the period
        FinancialSummaryDTO financialSummary = buildFinancialSummary(userId, range);

        // 2. Continuous 6-Month Monthly Trend (zero-filled for missing months)
        List<MonthlyTrendDTO> monthlyTrend = buildMonthlyTrend(userId, range);

        // 3. Top 5 Category Spending with proportions
        List<CategorySpendingDTO> categorySpending = buildCategorySpending(userId, range, financialSummary.totalExpense());

        // 4. Budget Overview for current month
        YearMonth currentYM = YearMonth.from(range.end());
        BudgetOverviewDTO budgetOverview = buildBudgetOverview(userId, currentYM);

        // 5. Recent 5 Transactions
        List<RecentTransactionDTO> recentTransactions = buildRecentTransactions(userId);

        // 6. Previous Month Comparison
        PreviousMonthComparisonDTO previousMonthComparison = buildPreviousMonthComparison(userId, currentYM);

        // 7. Deterministic Financial Insights
        List<FinancialInsightDTO> insights = generateInsights(
                financialSummary,
                categorySpending,
                budgetOverview,
                previousMonthComparison
        );

        return new DashboardResponse(
                financialSummary,
                monthlyTrend,
                categorySpending,
                budgetOverview,
                recentTransactions,
                previousMonthComparison,
                insights
        );
    }

    private FinancialSummaryDTO buildFinancialSummary(UUID userId, DateRange range) {
        FinancialSummaryAggregation agg = transactionRepository.getFinancialSummary(
                userId, range.start(), range.end());

        BigDecimal totalIncome = (agg != null && agg.getTotalIncome() != null)
                ? agg.getTotalIncome()
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalExpense = (agg != null && agg.getTotalExpense() != null)
                ? agg.getTotalExpense()
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        Long count = agg != null ? agg.getTransactionCount() : null;
        long transactionCount = count != null ? count : 0L;

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        return new FinancialSummaryDTO(
                totalIncome,
                totalExpense,
                netBalance,
                transactionCount,
                range.start(),
                range.end()
        );
    }

    private List<MonthlyTrendDTO> buildMonthlyTrend(UUID userId, DateRange range) {
        YearMonth targetEndYM = YearMonth.from(range.end());
        YearMonth startYM = targetEndYM.minusMonths(5);

        LocalDate trendStart = startYM.atDay(1);
        LocalDate trendEnd = targetEndYM.atEndOfMonth();

        List<MonthlyAggregation> monthlyAggs = transactionRepository.getMonthlySummary(
                userId, trendStart, trendEnd);

        Map<YearMonth, MonthlyAggregation> aggMap = new HashMap<>();
        for (MonthlyAggregation agg : monthlyAggs) {
            if (agg.getYear() != null && agg.getMonth() != null) {
                aggMap.put(YearMonth.of(agg.getYear(), agg.getMonth()), agg);
            }
        }

        List<MonthlyTrendDTO> trends = new ArrayList<>(6);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = targetEndYM.minusMonths(i);
            MonthlyAggregation found = aggMap.get(ym);

            BigDecimal totalIncome = (found != null && found.getTotalIncome() != null)
                    ? found.getTotalIncome()
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            BigDecimal totalExpense = (found != null && found.getTotalExpense() != null)
                    ? found.getTotalExpense()
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            BigDecimal netBalance = totalIncome.subtract(totalExpense);

            trends.add(new MonthlyTrendDTO(
                    ym.getYear(),
                    ym.getMonthValue(),
                    totalIncome,
                    totalExpense,
                    netBalance
            ));
        }

        return trends;
    }

    private List<CategorySpendingDTO> buildCategorySpending(UUID userId, DateRange range, BigDecimal totalExpense) {
        List<CategorySpendingAggregation> aggregations = transactionRepository.getSpendingByCategory(
                userId, range.start(), range.end());

        if (aggregations.isEmpty()) {
            return Collections.emptyList();
        }

        List<CategorySpendingAggregation> top5 = aggregations.stream()
                .limit(5)
                .toList();

        Set<UUID> categoryIds = top5.stream()
                .map(CategorySpendingAggregation::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategorySpendingDTO> list = new ArrayList<>(top5.size());
        for (CategorySpendingAggregation agg : top5) {
            BigDecimal percentage = calculatePercentage(agg.getTotalAmount(), totalExpense);
            String categoryName = categoryNameMap.getOrDefault(agg.getCategoryId(), "Unknown");

            list.add(new CategorySpendingDTO(
                    agg.getCategoryId(),
                    categoryName,
                    agg.getTotalAmount(),
                    percentage
            ));
        }

        return list;
    }

    private BudgetOverviewDTO buildBudgetOverview(UUID userId, YearMonth ym) {
        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(
                userId, (short) ym.getYear(), (short) ym.getMonthValue());

        if (budgets.isEmpty()) {
            return new BudgetOverviewDTO(
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                    0,
                    0,
                    0
            );
        }

        Set<UUID> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .collect(Collectors.toSet());

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CategorySpendingAggregation> spendingAggs = transactionRepository.sumAmountGroupedByCategory(
                userId, categoryIds, TransactionType.EXPENSE, start, end);

        Map<UUID, BigDecimal> spendingMap = spendingAggs.stream()
                .collect(Collectors.toMap(
                        CategorySpendingAggregation::getCategoryId,
                        CategorySpendingAggregation::getTotalAmount
                ));

        BigDecimal totalBudget = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalSpent = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        int normalCount = 0;
        int warningCount = 0;
        int exceededCount = 0;

        for (Budget b : budgets) {
            BigDecimal bAmount = b.getAmount();
            totalBudget = totalBudget.add(bAmount);

            BigDecimal bSpent = spendingMap.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
            totalSpent = totalSpent.add(bSpent);

            BigDecimal bUsage = calculatePercentage(bSpent, bAmount);
            ThresholdStatus status = ThresholdStatus.fromPercentage(bUsage);

            switch (status) {
                case NORMAL -> normalCount++;
                case WARNING -> warningCount++;
                case EXCEEDED -> exceededCount++;
            }
        }

        BigDecimal totalRemaining = totalBudget.subtract(totalSpent);
        BigDecimal overallUsagePercentage = calculatePercentage(totalSpent, totalBudget);

        return new BudgetOverviewDTO(
                totalBudget,
                totalSpent,
                totalRemaining,
                overallUsagePercentage,
                normalCount,
                warningCount,
                exceededCount
        );
    }

    private List<RecentTransactionDTO> buildRecentTransactions(UUID userId) {
        Pageable pageable = PageRequest.of(0, 5, Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("id")
        ));

        Page<Transaction> txPage = transactionRepository.findByUserId(userId, pageable);

        if (txPage.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> categoryIds = txPage.getContent().stream()
                .map(Transaction::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<RecentTransactionDTO> recentList = new ArrayList<>(txPage.getContent().size());
        for (Transaction tx : txPage.getContent()) {
            String categoryName = categoryNameMap.getOrDefault(tx.getCategoryId(), "Unknown");
            recentList.add(new RecentTransactionDTO(
                    tx.getId(),
                    tx.getCategoryId(),
                    categoryName,
                    tx.getAmount(),
                    tx.getType(),
                    tx.getDescription(),
                    tx.getTransactionDate()
            ));
        }

        return recentList;
    }

    private PreviousMonthComparisonDTO buildPreviousMonthComparison(UUID userId, YearMonth currentYM) {
        YearMonth previousYM = currentYM.minusMonths(1);

        FinancialSummaryAggregation currSummary = transactionRepository.getFinancialSummary(
                userId, currentYM.atDay(1), currentYM.atEndOfMonth());

        FinancialSummaryAggregation prevSummary = transactionRepository.getFinancialSummary(
                userId, previousYM.atDay(1), previousYM.atEndOfMonth());

        BigDecimal currentMonthExpense = (currSummary != null && currSummary.getTotalExpense() != null)
                ? currSummary.getTotalExpense()
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal previousMonthExpense = (prevSummary != null && prevSummary.getTotalExpense() != null)
                ? prevSummary.getTotalExpense()
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal expenseChangeAmount = currentMonthExpense.subtract(previousMonthExpense);

        BigDecimal expenseChangePercentage;
        if (previousMonthExpense.compareTo(BigDecimal.ZERO) <= 0) {
            if (currentMonthExpense.compareTo(BigDecimal.ZERO) <= 0) {
                expenseChangePercentage = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            } else {
                expenseChangePercentage = BigDecimal.valueOf(100).setScale(3, RoundingMode.HALF_UP);
            }
        } else {
            expenseChangePercentage = expenseChangeAmount.abs()
                    .divide(previousMonthExpense, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(3, RoundingMode.HALF_UP);
        }

        TrendDirection trend;
        if (currentMonthExpense.compareTo(previousMonthExpense) > 0) {
            trend = TrendDirection.INCREASED;
        } else if (currentMonthExpense.compareTo(previousMonthExpense) < 0) {
            trend = TrendDirection.DECREASED;
        } else {
            trend = TrendDirection.UNCHANGED;
        }

        return new PreviousMonthComparisonDTO(
                currentMonthExpense,
                previousMonthExpense,
                expenseChangeAmount,
                expenseChangePercentage,
                trend
        );
    }

    private List<FinancialInsightDTO> generateInsights(
            FinancialSummaryDTO summary,
            List<CategorySpendingDTO> categorySpending,
            BudgetOverviewDTO budgetOverview,
            PreviousMonthComparisonDTO comparison
    ) {
        List<FinancialInsightDTO> insights = new ArrayList<>();

        // 1. No Activity Check
        if (summary.transactionCount() == 0) {
            insights.add(new FinancialInsightDTO(
                    "NO_TRANSACTIONS",
                    "No Activity",
                    "No transactions recorded for the selected period yet.",
                    InsightSeverity.INFO
            ));
            return insights;
        }

        // 2. Budget Health Checks
        if (budgetOverview.exceededBudgetCount() > 0) {
            insights.add(new FinancialInsightDTO(
                    "BUDGET_EXCEEDED",
                    "Budget Limit Exceeded",
                    budgetOverview.exceededBudgetCount() + " budget category has exceeded its allocated limit.",
                    InsightSeverity.DANGER
            ));
        }

        if (budgetOverview.warningBudgetCount() > 0) {
            insights.add(new FinancialInsightDTO(
                    "BUDGET_WARNING",
                    "Budget Threshold Warning",
                    budgetOverview.warningBudgetCount() + " budget category has reached over 80% of its spending limit.",
                    InsightSeverity.WARNING
            ));
        }

        // 3. Negative Net Balance Check
        if (summary.netBalance().compareTo(BigDecimal.ZERO) < 0 && summary.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new FinancialInsightDTO(
                    "NEGATIVE_NET_BALANCE",
                    "Expenses Exceed Income",
                    "Your total expenses exceed your income by $" + summary.netBalance().abs().setScale(2, RoundingMode.HALF_UP) + " for this period.",
                    InsightSeverity.WARNING
            ));
        }

        // 4. Top Spending Category
        if (!categorySpending.isEmpty()) {
            CategorySpendingDTO top = categorySpending.get(0);
            if (top.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                insights.add(new FinancialInsightDTO(
                        "TOP_SPENDING_CATEGORY",
                        "Top Expense Category",
                        "Your highest spending category is " + top.categoryName() + " ($" + top.totalAmount().setScale(2, RoundingMode.HALF_UP) + "), accounting for " + top.percentage().setScale(1, RoundingMode.HALF_UP) + "% of total expenses.",
                        InsightSeverity.INFO
                ));
            }
        }

        // 5. Monthly Expense Trend Comparison
        if (comparison.previousMonthExpense().compareTo(BigDecimal.ZERO) > 0) {
            if (comparison.trend() == TrendDirection.INCREASED) {
                insights.add(new FinancialInsightDTO(
                        "EXPENSE_INCREASED",
                        "Spending Increased",
                        "Your monthly spending increased by " + comparison.expenseChangePercentage().setScale(1, RoundingMode.HALF_UP) + "% compared to last month.",
                        InsightSeverity.WARNING
                ));
            } else if (comparison.trend() == TrendDirection.DECREASED) {
                insights.add(new FinancialInsightDTO(
                        "EXPENSE_DECREASED",
                        "Spending Decreased",
                        "Your monthly spending decreased by " + comparison.expenseChangePercentage().setScale(1, RoundingMode.HALF_UP) + "% compared to last month.",
                        InsightSeverity.INFO
                ));
            }
        }

        return insights;
    }

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Invalid date range: 'from' date must be on or before 'to' date", HttpStatus.BAD_REQUEST);
        }

        if (from == null && to == null) {
            YearMonth current = YearMonth.now();
            return new DateRange(current.atDay(1), current.atEndOfMonth());
        }

        if (from != null && to == null) {
            return new DateRange(from, YearMonth.from(from).atEndOfMonth());
        }

        if (from == null) {
            return new DateRange(YearMonth.from(to).atDay(1), to);
        }

        return new DateRange(from, to);
    }

    private BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || part == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return part.divide(total, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(3, RoundingMode.HALF_UP);
    }

    public record DateRange(LocalDate start, LocalDate end) {}
}
