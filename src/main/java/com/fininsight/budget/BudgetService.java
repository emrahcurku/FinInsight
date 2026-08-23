package com.fininsight.budget;

import com.fininsight.budget.dto.BudgetResponse;
import com.fininsight.budget.dto.CreateBudgetRequest;
import com.fininsight.budget.dto.UpdateBudgetRequest;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.common.dto.PagedResponse;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.BudgetEvent;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.config.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing monthly category budgets, spending calculations,
 * and threshold monitoring.
 */
@Slf4j
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CacheEvictionService cacheEvictionService;
    private final DomainEventPublisher domainEventPublisher;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            CacheEvictionService cacheEvictionService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * Creates a new monthly budget after verifying category accessibility and period uniqueness.
     */
    @Transactional
    public BudgetResponse createBudget(UUID userId, CreateBudgetRequest request) {
        // 1. Verify category exists and is accessible
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.isSystemCategory() && !category.getUserId().equals(userId)) {
            log.warn("Unauthorized category access attempt for budget creation: user {} tried to use category {}", userId, request.categoryId());
            throw new BusinessException("Access to this category is forbidden", HttpStatus.FORBIDDEN);
        }

        // 2. Enforce duplicate period constraint at application level (DB unique constraint is final safety)
        if (budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(
                userId, request.categoryId(), (short) request.year(), (short) request.month())) {
            throw new BusinessException("A budget for this category and period already exists", HttpStatus.CONFLICT);
        }

        // 3. Save budget
        Budget budget = new Budget(userId, request.categoryId(), request.amount(), request.month(), request.year());
        Budget savedBudget = budgetRepository.save(budget);
        log.info("Budget created with id: {} for user: {}, category: {}, year: {}, month: {}",
                savedBudget.getId(), userId, request.categoryId(), request.year(), request.month());

        cacheEvictionService.evictUserBudgetCaches(userId);

        domainEventPublisher.publish(BudgetEvent.created(
                userId,
                savedBudget.getId(),
                savedBudget.getCategoryId(),
                savedBudget.getAmount(),
                savedBudget.getMonth(),
                savedBudget.getYear(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        // 4. Compute metrics and return response
        return buildBudgetResponse(savedBudget, category.getName());
    }

    /**
     * Retrieves a single budget by ID for the authenticated user.
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        Category category = categoryRepository.findById(budget.getCategoryId()).orElse(null);
        String categoryName = category != null ? category.getName() : "Unknown";

        return buildBudgetResponse(budget, categoryName);
    }

    /**
     * Lists paginated budgets matching optional filters with batch category lookups and spending aggregations.
     * Prevents N+1 queries by batching category and spending lookups.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BudgetResponse> listBudgets(
            UUID userId,
            Integer year,
            Integer month,
            UUID categoryId,
            Pageable pageable
    ) {
        Specification<Budget> spec = BudgetSpecification.withFilters(userId, year, month, categoryId);
        Page<Budget> budgetPage = budgetRepository.findAll(spec, pageable);

        if (budgetPage.isEmpty()) {
            return PagedResponse.of(budgetPage, List.<BudgetResponse>of());
        }

        // 1. Batch resolve category names in a single query
        Set<UUID> categoryIds = budgetPage.getContent().stream()
                .map(Budget::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 2. Batch resolve spending per distinct (YearMonth, CategoryId) in 1 aggregation query per period
        Map<YearMonth, Set<UUID>> periodCategoryMap = new HashMap<>();
        for (Budget b : budgetPage.getContent()) {
            YearMonth ym = YearMonth.of(b.getYear(), b.getMonth());
            periodCategoryMap.computeIfAbsent(ym, k -> new HashSet<>()).add(b.getCategoryId());
        }

        Map<String, BigDecimal> spendingMap = new HashMap<>();
        for (Map.Entry<YearMonth, Set<UUID>> entry : periodCategoryMap.entrySet()) {
            YearMonth ym = entry.getKey();
            Set<UUID> catIds = entry.getValue();
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            List<CategorySpendingAggregation> aggregations = transactionRepository.sumAmountGroupedByCategory(
                    userId, catIds, TransactionType.EXPENSE, start, end
            );

            for (CategorySpendingAggregation agg : aggregations) {
                spendingMap.put(ym + "_" + agg.getCategoryId(), agg.getTotalAmount());
            }
        }

        // 3. Assemble response DTOs using pre-fetched lookups
        List<BudgetResponse> responseList = budgetPage.getContent().stream()
                .map(b -> {
                    String categoryName = categoryNameMap.getOrDefault(b.getCategoryId(), "Unknown");
                    YearMonth ym = YearMonth.of(b.getYear(), b.getMonth());
                    BigDecimal spent = spendingMap.getOrDefault(ym + "_" + b.getCategoryId(), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                    BigDecimal remaining = b.getAmount().subtract(spent);
                    BigDecimal usagePercentage = calculateUsagePercentage(spent, b.getAmount());
                    ThresholdStatus status = ThresholdStatus.fromPercentage(usagePercentage);

                    return new BudgetResponse(
                            b.getId(),
                            b.getCategoryId(),
                            categoryName,
                            b.getAmount(),
                            b.getYear(),
                            b.getMonth(),
                            spent,
                            remaining,
                            usagePercentage,
                            status,
                            b.getCreatedAt(),
                            b.getUpdatedAt()
                    );
                })
                .toList();

        return PagedResponse.of(budgetPage, responseList);
    }

    /**
     * Updates the amount limit for an existing budget.
     */
    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        budget.setAmount(request.amount());
        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Budget updated with id: {} for user: {}", budgetId, userId);

        cacheEvictionService.evictUserBudgetCaches(userId);

        domainEventPublisher.publish(BudgetEvent.updated(
                userId,
                updatedBudget.getId(),
                updatedBudget.getCategoryId(),
                updatedBudget.getAmount(),
                updatedBudget.getMonth(),
                updatedBudget.getYear(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        Category category = categoryRepository.findById(budget.getCategoryId()).orElse(null);
        String categoryName = category != null ? category.getName() : "Unknown";

        return buildBudgetResponse(updatedBudget, categoryName);
    }

    /**
     * Deletes an existing budget owned by the authenticated user.
     */
    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        budgetRepository.delete(budget);
        log.info("Budget deleted with id: {} for user: {}", budgetId, userId);

        cacheEvictionService.evictUserBudgetCaches(userId);

        domainEventPublisher.publish(BudgetEvent.deleted(
                userId,
                budget.getId(),
                budget.getCategoryId(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }

    /**
     * Helper to compute spending metrics and assemble a single BudgetResponse.
     */
    private BudgetResponse buildBudgetResponse(Budget budget, String categoryName) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal spentAmount = transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                budget.getUserId(), budget.getCategoryId(), TransactionType.EXPENSE, start, end
        );

        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal remainingAmount = budget.getAmount().subtract(spentAmount);
        BigDecimal usagePercentage = calculateUsagePercentage(spentAmount, budget.getAmount());
        ThresholdStatus thresholdStatus = ThresholdStatus.fromPercentage(usagePercentage);

        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                categoryName,
                budget.getAmount(),
                budget.getYear(),
                budget.getMonth(),
                spentAmount,
                remainingAmount,
                usagePercentage,
                thresholdStatus,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    /**
     * Calculates usage percentage: (spent / budget) * 100 with scale 3 and HALF_UP rounding.
     */
    private BigDecimal calculateUsagePercentage(BigDecimal spent, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return spent.divide(budget, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(3, RoundingMode.HALF_UP);
    }
}
