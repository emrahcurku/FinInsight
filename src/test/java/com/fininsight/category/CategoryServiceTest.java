package com.fininsight.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.dto.CategoryResponse;
import com.fininsight.category.dto.CreateCategoryRequest;
import com.fininsight.category.dto.UpdateCategoryRequest;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.CategoryEvent;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private CategoryService categoryService;

    private UUID userId;
    private UUID otherUserId;
    private UUID systemCategoryId;
    private UUID userCategoryId;
    private Category systemCategory;
    private Category userCategory;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        systemCategoryId = UUID.randomUUID();
        systemCategory = new Category("Groceries", CategoryType.EXPENSE);
        systemCategory.setId(systemCategoryId);

        userCategoryId = UUID.randomUUID();
        userCategory = new Category(userId, "My Custom Cat", CategoryType.EXPENSE);
        userCategory.setId(userCategoryId);
    }

    @Test
    @DisplayName("List categories returns system and own categories")
    void testGetCategories() {
        when(categoryRepository.findAllAvailableForUser(userId)).thenReturn(List.of(systemCategory, userCategory));

        List<CategoryResponse> result = categoryService.getCategories(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Groceries");
        assertThat(result.get(0).system()).isTrue();
        assertThat(result.get(1).name()).isEqualTo("My Custom Cat");
        assertThat(result.get(1).system()).isFalse();
    }

    @Test
    @DisplayName("Get system category by ID succeeds")
    void testGetSystemCategorySuccess() {
        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));

        CategoryResponse response = categoryService.getCategory(userId, systemCategoryId);

        assertThat(response.id()).isEqualTo(systemCategoryId);
        assertThat(response.system()).isTrue();
    }

    @Test
    @DisplayName("Get own custom category succeeds")
    void testGetOwnCategorySuccess() {
        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));

        CategoryResponse response = categoryService.getCategory(userId, userCategoryId);

        assertThat(response.id()).isEqualTo(userCategoryId);
        assertThat(response.system()).isFalse();
    }

    @Test
    @DisplayName("Get other user's category returns 404 Not Found (IDOR)")
    void testGetOtherUserCategoryReturns404() {
        Category otherCategory = new Category(otherUserId, "Secret", CategoryType.EXPENSE);
        UUID otherCatId = UUID.randomUUID();
        otherCategory.setId(otherCatId);

        when(categoryRepository.findById(otherCatId)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() -> categoryService.getCategory(userId, otherCatId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("Create custom category succeeds")
    void testCreateCategorySuccess() {
        CreateCategoryRequest request = new CreateCategoryRequest("  Fitness  ", CategoryType.EXPENSE);

        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Fitness", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.existsByUserIdIsNullAndNameIgnoreCaseAndType("Fitness", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse response = categoryService.createCategory(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Fitness");
        assertThat(response.system()).isFalse();
        verify(cacheEvictionService).evictUserCategoryCaches(userId);
        verify(domainEventPublisher).publish(any(CategoryEvent.class));
    }

    @Test
    @DisplayName("Create category with duplicate name returns 409 Conflict")
    void testCreateDuplicateCategoryReturns409() {
        CreateCategoryRequest request = new CreateCategoryRequest("Groceries", CategoryType.EXPENSE);

        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Groceries", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.existsByUserIdIsNullAndNameIgnoreCaseAndType("Groceries", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Update own custom category succeeds")
    void testUpdateOwnCategorySuccess() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Updated Hobby");

        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Updated Hobby", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.existsByUserIdIsNullAndNameIgnoreCaseAndType("Updated Hobby", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(userId, userCategoryId, request);

        assertThat(response.name()).isEqualTo("Updated Hobby");
        verify(cacheEvictionService).evictUserCategoryCaches(userId);
        verify(domainEventPublisher).publish(any(CategoryEvent.class));
    }

    @Test
    @DisplayName("Update system category throws 403 Forbidden")
    void testUpdateSystemCategoryForbidden() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Hacked");

        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(userId, systemCategoryId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("System categories cannot be modified");
                });
    }

    @Test
    @DisplayName("Delete own category succeeds when not in use")
    void testDeleteOwnCategorySuccess() {
        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(transactionRepository.existsByCategoryId(userCategoryId)).thenReturn(false);
        when(budgetRepository.existsByCategoryId(userCategoryId)).thenReturn(false);

        categoryService.deleteCategory(userId, userCategoryId);

        verify(categoryRepository).delete(userCategory);
        verify(cacheEvictionService).evictUserCategoryCaches(userId);
        verify(domainEventPublisher).publish(any(CategoryEvent.class));
    }

    @Test
    @DisplayName("Delete system category throws 403 Forbidden")
    void testDeleteSystemCategoryForbidden() {
        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(userId, systemCategoryId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("System categories cannot be deleted");
                });
    }

    @Test
    @DisplayName("Delete category in use by transactions throws 409 Conflict")
    void testDeleteCategoryInUseByTransactionsThrows409() {
        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(transactionRepository.existsByCategoryId(userCategoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(userId, userCategoryId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("in use by transactions");
                });
    }

    @Test
    @DisplayName("Delete category in use by budgets throws 409 Conflict")
    void testDeleteCategoryInUseByBudgetsThrows409() {
        when(categoryRepository.findById(userCategoryId)).thenReturn(Optional.of(userCategory));
        when(transactionRepository.existsByCategoryId(userCategoryId)).thenReturn(false);
        when(budgetRepository.existsByCategoryId(userCategoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(userId, userCategoryId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("in use by budgets");
                });
    }
}
