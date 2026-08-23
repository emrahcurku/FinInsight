package com.fininsight.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fininsight.analytics.dto.BudgetOverviewResponse;
import com.fininsight.analytics.dto.CategorySpendingResponse;
import com.fininsight.analytics.dto.FinancialSummaryAggregation;
import com.fininsight.analytics.dto.FinancialSummaryResponse;
import com.fininsight.analytics.dto.MonthlyAggregation;
import com.fininsight.analytics.dto.MonthlySummaryResponse;
import com.fininsight.analytics.dto.TopCategoryResponse;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.budget.ThresholdStatus;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.config.CacheNames;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service orchestrating financial analytics calculations and aggregations.
 * All queries enforce strict user data isolation and leverage database-level aggregation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    /**
     * Calculates overall income, expense, net balance, and transaction count for a period.
     */
    @Cacheable(value = CacheNames.ANALYTICS_SUMMARY, key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')")
    @Transactional(readOnly = true)
    public FinancialSummaryResponse getFinancialSummary(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = resolveDateRange(from, to);

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

        return new FinancialSummaryResponse(
                totalIncome,
                totalExpense,
                netBalance,
                transactionCount,   
                range.start(),
                range.end()
        );
    }

    /**
     * Aggregates expense breakdown per category with relative percentages.
     */
    @Cacheable(value = CacheNames.ANALYTICS_CATEGORY, key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')")
    @Transactional(readOnly = true)
    public List<CategorySpendingResponse> getSpendingByCategory(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = resolveDateRange(from, to);

        List<CategorySpendingAggregation> aggregations = transactionRepository.getSpendingByCategory(
                userId, range.start(), range.end());

        if (aggregations.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal totalExpense = aggregations.stream()
                .map(CategorySpendingAggregation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<UUID> categoryIds = aggregations.stream()
                .map(CategorySpendingAggregation::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategorySpendingResponse> responses = new ArrayList<>(aggregations.size());
        for (CategorySpendingAggregation agg : aggregations) {
            BigDecimal percentage = calculatePercentage(agg.getTotalAmount(), totalExpense);
            String categoryName = categoryNameMap.getOrDefault(agg.getCategoryId(), "Unknown");

            responses.add(new CategorySpendingResponse(
                    agg.getCategoryId(),
                    categoryName,
                    agg.getTotalAmount(),
                    percentage
            ));
        }

        return responses;
    }

    /**
     * Groups transaction income and expense by year and month.
     */
    @Cacheable(value = CacheNames.ANALYTICS_MONTHLY, key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')")
    @Transactional(readOnly = true)
    public List<MonthlySummaryResponse> getMonthlySummary(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = resolveDateRange(from, to);

        List<MonthlyAggregation> aggregations = transactionRepository.getMonthlySummary(
                userId, range.start(), range.end());

        if (aggregations.isEmpty()) {
            return Collections.emptyList();
        }

        List<MonthlySummaryResponse> responses = new ArrayList<>(aggregations.size());
        for (MonthlyAggregation agg : aggregations) {
            BigDecimal totalIncome = agg.getTotalIncome() != null
                    ? agg.getTotalIncome()
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            BigDecimal totalExpense = agg.getTotalExpense() != null
                    ? agg.getTotalExpense()
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            BigDecimal netBalance = totalIncome.subtract(totalExpense);

            Integer yearObj = agg.getYear();
            int aggYear = (yearObj != null) ? yearObj : 0;
            Integer monthObj = agg.getMonth();
            int aggMonth = (monthObj != null) ? monthObj : 0;
            
            responses.add(new MonthlySummaryResponse(
                    aggYear,
                    aggMonth,
                    totalIncome,
                    totalExpense,
                    netBalance
            ));
        }

        return responses;
    }

    /**
     * Computes overall budget status, totals, and threshold status distribution for a period.
     */
    @Cacheable(value = CacheNames.ANALYTICS_BUDGET_OVERVIEW, key = "#userId + ':' + #year + ':' + #month")
    @Transactional(readOnly = true)
    public BudgetOverviewResponse getBudgetOverview(UUID userId, Integer year, Integer month) {
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        int resolvedMonth = (month != null) ? month : LocalDate.now().getMonthValue();

        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BusinessException("Month must be between 1 and 12", HttpStatus.BAD_REQUEST);
        }
        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BusinessException("Year must be between 2000 and 2100", HttpStatus.BAD_REQUEST);
        }

        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(
                userId, (short) resolvedYear, (short) resolvedMonth);

        if (budgets.isEmpty()) {
            return new BudgetOverviewResponse(
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

        YearMonth ym = YearMonth.of(resolvedYear, resolvedMonth);
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

            BigDecimal bUsage = calculateUsagePercentage(bSpent, bAmount);
            ThresholdStatus status = ThresholdStatus.fromPercentage(bUsage);

            switch (status) {
                case NORMAL -> normalCount++;
                case WARNING -> warningCount++;
                case EXCEEDED -> exceededCount++;
            }
        }

        BigDecimal totalRemaining = totalBudget.subtract(totalSpent);
        BigDecimal overallUsagePercentage = calculateUsagePercentage(totalSpent, totalBudget);

        return new BudgetOverviewResponse(
                totalBudget,
                totalSpent,
                totalRemaining,
                overallUsagePercentage,
                normalCount,
                warningCount,
                exceededCount
        );
    }

    /**
     * Finds the category with the highest expense amount for the specified period.
     */
    @Cacheable(value = CacheNames.ANALYTICS_TOP_CATEGORIES, key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')")
    @Transactional(readOnly = true)
    public TopCategoryResponse getTopCategory(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = resolveDateRange(from, to);

        List<CategorySpendingAggregation> aggregations = transactionRepository.getSpendingByCategory(
                userId, range.start(), range.end());

        if (aggregations.isEmpty() || aggregations.get(0).getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResourceNotFoundException("No expense transactions found for the specified period");
        }

        CategorySpendingAggregation top = aggregations.get(0);
        Category category = categoryRepository.findById(top.getCategoryId()).orElse(null);
        String categoryName = (category != null) ? category.getName() : "Unknown";

        return new TopCategoryResponse(
                top.getCategoryId(),
                categoryName,
                top.getTotalAmount()
        );
    }

    /**
     * Resolves and validates date ranges, providing deterministic defaults when parameters are absent.
     */
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

    private BigDecimal calculateUsagePercentage(BigDecimal spent, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0 || spent == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return spent.divide(budget, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(3, RoundingMode.HALF_UP);
    }

    public record DateRange(LocalDate start, LocalDate end) {}
}
