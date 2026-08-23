package com.fininsight;

import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.transaction.Transaction;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.user.Role;
import com.fininsight.user.User;
import com.fininsight.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseSchemaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Test
    @DisplayName("Flyway V2 default system categories should be pre-populated")
    void testSystemDefaultCategoriesExist() {
        List<Category> systemCategories = categoryRepository.findByUserIdIsNull();
        assertThat(systemCategories).isNotEmpty();
        assertThat(systemCategories.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("User creation and unique email constraint")
    void testUserCreationAndUniqueEmail() {
        User user1 = new User("test.user@example.com", "hashed_password", "Test", "User");
        userRepository.saveAndFlush(user1);

        assertThat(user1.getId()).isNotNull();
        assertThat(user1.getCreatedAt()).isNotNull();
        assertThat(user1.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user1.isEnabled()).isTrue();

        User user2 = new User("test.user@example.com", "another_hash", "Another", "User");
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Transaction creation and category association")
    void testTransactionCreation() {
        User user = userRepository.saveAndFlush(
                new User("tx.user@example.com", "hash", "Tx", "User"));

        List<Category> categories = categoryRepository.findAllAvailableForUser(user.getId());
        assertThat(categories).isNotEmpty();
        Category expenseCategory = categories.stream()
                .filter(c -> c.getType() == CategoryType.EXPENSE)
                .findFirst()
                .orElseThrow();

        Transaction tx = new Transaction(
                user.getId(),
                expenseCategory.getId(),
                new BigDecimal("150.5000"),
                TransactionType.EXPENSE,
                "Grocery shopping",
                LocalDate.now()
        );
        transactionRepository.saveAndFlush(tx);

        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("150.5000"));

        Optional<Transaction> fetched = transactionRepository.findByIdAndUserId(tx.getId(), user.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Grocery shopping");
    }

    @Test
    @DisplayName("Budget creation and unique period constraint per user and category")
    void testBudgetCreationAndUniqueConstraint() {
        User user = userRepository.saveAndFlush(
                new User("budget.user@example.com", "hash", "Budget", "User"));

        Category category = categoryRepository.findAllAvailableForUser(user.getId()).getFirst();

        Budget budget1 = new Budget(user.getId(), category.getId(), new BigDecimal("5000.0000"), (short) 8, (short) 2026);
        budgetRepository.saveAndFlush(budget1);
        assertThat(budget1.getId()).isNotNull();

        // Duplicate budget for same user + category + year + month should fail
        Budget duplicateBudget = new Budget(user.getId(), category.getId(), new BigDecimal("6000.0000"), (short) 8, (short) 2026);
        assertThatThrownBy(() -> budgetRepository.saveAndFlush(duplicateBudget))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
