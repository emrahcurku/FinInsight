package com.fininsight.transaction;

import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.common.dto.PagedResponse;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.dto.CreateTransactionRequest;
import com.fininsight.transaction.dto.TransactionResponse;
import com.fininsight.transaction.dto.UpdateTransactionRequest;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.common.event.TransactionEvent;
import com.fininsight.config.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing financial transactions.
 * Enforces user ownership boundaries, category access controls, and validation.
 */
@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CacheEvictionService cacheEvictionService;
    private final DomainEventPublisher domainEventPublisher;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            CacheEvictionService cacheEvictionService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * Creates a new transaction for the authenticated user.
     * Validates that the referenced category is either a system category or owned by the user.
     */
    @Transactional
    public TransactionResponse createTransaction(UUID userId, CreateTransactionRequest request) {
        Category category = validateCategoryAccessibility(request.categoryId(), userId);

        Transaction transaction = new Transaction(
                userId,
                request.categoryId(),
                request.amount(),
                request.type(),
                request.description(),
                request.transactionDate()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created with id: {} for user: {}", savedTransaction.getId(), userId);

        cacheEvictionService.evictUserTransactionCaches(userId);

        domainEventPublisher.publish(TransactionEvent.created(
                userId,
                savedTransaction.getId(),
                savedTransaction.getCategoryId(),
                savedTransaction.getAmount(),
                savedTransaction.getType().name(),
                savedTransaction.getTransactionDate(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        return TransactionResponse.fromEntity(savedTransaction, category.getName());
    }

    /**
     * Retrieves a single transaction by ID for the authenticated user.
     * Throws ResourceNotFoundException (404) if not found or belongs to another user (IDOR prevention).
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        String categoryName = categoryRepository.findById(transaction.getCategoryId())
                .map(Category::getName)
                .orElse("Unknown");

        return TransactionResponse.fromEntity(transaction, categoryName);
    }

    /**
     * Retrieves a paginated list of transactions matching optional filters for the authenticated user.
     * Resolves category names in a single batch query to prevent N+1 queries.
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> listTransactions(
            UUID userId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            UUID categoryId,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        Specification<Transaction> spec = TransactionSpecification.withFilters(userId, from, to, type, categoryId);
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        // Batch fetch all distinct category names for the current page (prevents N+1 query problem)
        Set<UUID> categoryIds = page.getContent().stream()
                .map(Transaction::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Page<TransactionResponse> responsePage = page.map(t ->
                TransactionResponse.fromEntity(t, categoryNameMap.getOrDefault(t.getCategoryId(), "Unknown"))
        );

        return PagedResponse.of(responsePage);
    }

    /**
     * Updates an existing transaction for the authenticated user.
     * Validates ownership and accessibility of any updated category.
     */
    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Category category = validateCategoryAccessibility(request.categoryId(), userId);

        transaction.setCategoryId(request.categoryId());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Transaction updated with id: {} for user: {}", transactionId, userId);

        cacheEvictionService.evictUserTransactionCaches(userId);

        domainEventPublisher.publish(TransactionEvent.updated(
                userId,
                updatedTransaction.getId(),
                updatedTransaction.getCategoryId(),
                updatedTransaction.getAmount(),
                updatedTransaction.getType().name(),
                updatedTransaction.getTransactionDate(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));

        return TransactionResponse.fromEntity(updatedTransaction, category.getName());
    }

    /**
     * Deletes a transaction by ID for the authenticated user.
     * Throws ResourceNotFoundException (404) if not found or belongs to another user.
     */
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transactionRepository.delete(transaction);
        log.info("Transaction deleted with id: {} for user: {}", transactionId, userId);

        cacheEvictionService.evictUserTransactionCaches(userId);

        domainEventPublisher.publish(TransactionEvent.deleted(
                userId,
                transaction.getId(),
                transaction.getCategoryId(),
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }

    /**
     * Validates that the category exists and is accessible to the user
     * (either a global system category or owned by the authenticated user).
     */
    private Category validateCategoryAccessibility(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found", HttpStatus.NOT_FOUND));

        if (category.getUserId() != null && !category.getUserId().equals(userId)) {
            log.warn("Unauthorized category access attempt: user {} tried to use category {} owned by user {}",
                    userId, categoryId, category.getUserId());
            throw new BusinessException("Access to this category is forbidden", HttpStatus.FORBIDDEN);
        }

        return category;
    }
}
