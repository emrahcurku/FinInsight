package com.fininsight.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.dto.PagedResponse;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.common.event.TransactionEvent;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.dto.CreateTransactionRequest;
import com.fininsight.transaction.dto.TransactionResponse;
import com.fininsight.transaction.dto.UpdateTransactionRequest;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID systemCategoryId;
    private UUID userCategoryId;
    private UUID otherUserCategoryId;
    private Category systemCategory;
    private Category userCategory;
    private Category otherUserCategory;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();

        systemCategoryId = UUID.randomUUID();
        systemCategory = new Category("Groceries", CategoryType.EXPENSE);
        systemCategory.setId(systemCategoryId);

        userCategoryId = UUID.randomUUID();
        userCategory = new Category(userId, "Custom Hobby", CategoryType.EXPENSE);
        userCategory.setId(userCategoryId);

        otherUserCategoryId = UUID.randomUUID();
        otherUserCategory = new Category(UUID.randomUUID(), "Secret Project", CategoryType.EXPENSE);
        otherUserCategory.setId(otherUserCategoryId);
    }

    @Test
    @DisplayName("Create transaction with system category succeeds")
    void testCreateTransactionWithSystemCategory() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                systemCategoryId,
                new BigDecimal("125.50"),
                TransactionType.EXPENSE,
                "Supermarket",
                LocalDate.now()
        );

        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(systemCategoryId);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.amount()).isEqualByComparingTo("125.50");
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        verify(cacheEvictionService).evictUserTransactionCaches(userId);
        verify(domainEventPublisher).publish(any(TransactionEvent.class));
    }

    @Test
    @DisplayName("Create transaction with user's own custom category succeeds")
    void testCreateTransactionWithOwnCategory() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                userCategoryId,
                new BigDecimal("500.00"),
                TransactionType.EXPENSE,
                "Guitar strings",
                LocalDate.now()
        );

        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.categoryName()).isEqualTo("Custom Hobby");
        verify(domainEventPublisher).publish(any(TransactionEvent.class));
    }

    @Test
    @DisplayName("Create transaction with another user's custom category throws 403 Forbidden")
    void testCreateTransactionWithAnotherUserCategoryForbidden() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                otherUserCategoryId,
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                "Unauthorized use",
                LocalDate.now()
        );

        when(categoryRepository.findById(otherUserCategoryId)).thenReturn(Optional.of(otherUserCategory));

        assertThatThrownBy(() -> transactionService.createTransaction(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("Access to this category is forbidden");
                });
    }

    @Test
    @DisplayName("Get own transaction by ID succeeds")
    void testGetOwnTransactionSuccess() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(userId, systemCategoryId, new BigDecimal("100.00"),
                TransactionType.EXPENSE, "Test", LocalDate.now());
        transaction.setId(transactionId);

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));

        TransactionResponse response = transactionService.getTransaction(userId, transactionId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(transactionId);
        assertThat(response.categoryName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("Get another user's transaction returns 404 Not Found (IDOR prevention)")
    void testGetAnotherUserTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(userId, transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    @DisplayName("List transactions with invalid date range throws 400 Bad Request")
    void testListTransactionsInvalidDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 23);
        LocalDate to = LocalDate.of(2026, 8, 1);
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> transactionService.listTransactions(userId, from, to, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("Start date cannot be after end date");
                });
    }

    @Test
    @DisplayName("List transactions resolves category names in batch without N+1 queries")
    void testListTransactionsBatchCategoryResolution() {
        Pageable pageable = PageRequest.of(0, 20);
        Transaction t1 = new Transaction(userId, systemCategoryId, new BigDecimal("10.00"), TransactionType.EXPENSE, "Item 1", LocalDate.now());
        t1.setId(UUID.randomUUID());
        Transaction t2 = new Transaction(userId, userCategoryId, new BigDecimal("20.00"), TransactionType.EXPENSE, "Item 2", LocalDate.now());
        t2.setId(UUID.randomUUID());

        Page<Transaction> mockPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(pageable))).thenReturn(mockPage);
        when(categoryRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any())).thenReturn(List.of(systemCategory, userCategory));

        PagedResponse<TransactionResponse> response = transactionService.listTransactions(userId, null, null, null, null, pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().get(0).categoryName()).isEqualTo("Groceries");
        assertThat(response.content().get(1).categoryName()).isEqualTo("Custom Hobby");
    }

    @Test
    @DisplayName("Update own transaction succeeds")
    void testUpdateOwnTransactionSuccess() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(userId, systemCategoryId, new BigDecimal("100.00"),
                TransactionType.EXPENSE, "Old Desc", LocalDate.now());
        transaction.setId(transactionId);

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                userCategoryId,
                new BigDecimal("200.00"),
                TransactionType.EXPENSE,
                "New Desc",
                LocalDate.now()
        );

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(userId, transactionId, updateReq);

        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.description()).isEqualTo("New Desc");
        assertThat(response.categoryName()).isEqualTo("Custom Hobby");
        verify(cacheEvictionService).evictUserTransactionCaches(userId);
        verify(domainEventPublisher).publish(any(TransactionEvent.class));
    }

    @Test
    @DisplayName("Update another user's transaction returns 404 Not Found")
    void testUpdateAnotherUserTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                systemCategoryId,
                new BigDecimal("200.00"),
                TransactionType.EXPENSE,
                "New Desc",
                LocalDate.now()
        );

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(userId, transactionId, updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    @DisplayName("Delete own transaction succeeds")
    void testDeleteOwnTransactionSuccess() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(userId, systemCategoryId, new BigDecimal("100.00"),
                TransactionType.EXPENSE, "Desc", LocalDate.now());
        transaction.setId(transactionId);

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(userId, transactionId);

        verify(transactionRepository).delete(transaction);
        verify(cacheEvictionService).evictUserTransactionCaches(userId);
        verify(domainEventPublisher).publish(any(TransactionEvent.class));
    }

    @Test
    @DisplayName("Delete another user's transaction returns 404 Not Found")
    void testDeleteAnotherUserTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(userId, transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }
}
