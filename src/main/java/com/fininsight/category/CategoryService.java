package com.fininsight.category;

import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.dto.CategoryResponse;
import com.fininsight.category.dto.CreateCategoryRequest;
import com.fininsight.category.dto.UpdateCategoryRequest;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.config.CacheNames;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.common.event.CategoryEvent;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.config.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing income and expense categories.
 * Handles dual-mode access (system default vs. user custom categories),
 * system category immutability, usage integrity checks, and IDOR protection.
 */
@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CacheEvictionService cacheEvictionService;
    private final DomainEventPublisher domainEventPublisher;

    public CategoryService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            CacheEvictionService cacheEvictionService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * Lists all categories available to the user (global system categories + own custom categories)
     * sorted deterministically by name and ID.
     */
    @Cacheable(value = CacheNames.CATEGORIES, key = "#userId")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID userId) {
        return categoryRepository.findAllAvailableForUser(userId).stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single category by ID.
     * Accessible if system category or owned by the user.
     * Throws ResourceNotFoundException (404) if category belongs to another user (IDOR protection).
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.isSystemCategory() && !category.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }

        return CategoryResponse.fromEntity(category);
    }

    /**
     * Creates a new custom category for the user after validating uniqueness.
     */
    @Transactional
    public CategoryResponse createCategory(UUID userId, CreateCategoryRequest request) {
        String normalizedName = request.name().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, normalizedName, request.type())
                || categoryRepository.existsByUserIdIsNullAndNameIgnoreCaseAndType(normalizedName, request.type())) {
            throw new BusinessException("A category with this name and type already exists", HttpStatus.CONFLICT);
        }

        Category category = new Category(userId, normalizedName, request.type());
        Category savedCategory = categoryRepository.save(category);
        log.info("Custom category created with id: {} for user: {}", savedCategory.getId(), userId);

        cacheEvictionService.evictUserCategoryCaches(userId);

        domainEventPublisher.publish(CategoryEvent.created(
                userId,
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getType().name(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        return CategoryResponse.fromEntity(savedCategory);
    }

    /**
     * Updates an existing custom category name.
     * Throws 403 Forbidden if attempting to update a system category.
     * Throws 404 Not Found if category belongs to another user.
     */
    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isSystemCategory()) {
            throw new BusinessException("System categories cannot be modified", HttpStatus.FORBIDDEN);
        }

        if (!category.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }

        String normalizedName = request.name().trim();
        if (!normalizedName.equalsIgnoreCase(category.getName())) {
            if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, normalizedName, category.getType())
                    || categoryRepository.existsByUserIdIsNullAndNameIgnoreCaseAndType(normalizedName, category.getType())) {
                throw new BusinessException("A category with this name and type already exists", HttpStatus.CONFLICT);
            }
        }

        category.setName(normalizedName);
        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated with id: {} for user: {}", categoryId, userId);

        cacheEvictionService.evictUserCategoryCaches(userId);

        domainEventPublisher.publish(CategoryEvent.updated(
                userId,
                updatedCategory.getId(),
                updatedCategory.getName(),
                updatedCategory.getType().name(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        return CategoryResponse.fromEntity(updatedCategory);
    }

    /**
     * Deletes a custom category if it is not referenced by any transactions or budgets.
     * Throws 403 Forbidden if attempting to delete a system category.
     * Throws 404 Not Found if category belongs to another user.
     * Throws 409 Conflict if category is in use.
     */
    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isSystemCategory()) {
            throw new BusinessException("System categories cannot be deleted", HttpStatus.FORBIDDEN);
        }

        if (!category.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException("Category is currently in use by transactions and cannot be deleted", HttpStatus.CONFLICT);
        }

        if (budgetRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException("Category is currently in use by budgets and cannot be deleted", HttpStatus.CONFLICT);
        }

        categoryRepository.delete(category);
        log.info("Category deleted with id: {} for user: {}", categoryId, userId);

        cacheEvictionService.evictUserCategoryCaches(userId);

        domainEventPublisher.publish(CategoryEvent.deleted(
                userId,
                category.getId(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
}
