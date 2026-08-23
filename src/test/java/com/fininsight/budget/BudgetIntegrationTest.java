package com.fininsight.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.auth.JwtTokenProvider;
import com.fininsight.budget.dto.CreateBudgetRequest;
import com.fininsight.budget.dto.UpdateBudgetRequest;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.transaction.Transaction;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.user.Role;
import com.fininsight.user.User;
import com.fininsight.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class BudgetIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private String tokenA;
    private Category systemCategory;
    private Category userBCategory;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        userA = userRepository.save(new User("user.a@example.com", "passA", "User", "A", Role.ROLE_USER));
        tokenA = jwtTokenProvider.generateAccessToken(userA.getId(), userA.getEmail(), userA.getRole());

        userB = userRepository.save(new User("user.b@example.com", "passB", "User", "B", Role.ROLE_USER));

        // 2. Create Categories
        systemCategory = categoryRepository.save(new Category("Groceries", CategoryType.EXPENSE));
        userBCategory = categoryRepository.save(new Category(userB.getId(), "User B Secret Cat", CategoryType.EXPENSE));
    }

    @Test
    @DisplayName("Create budget succeeds (201 Created)")
    void testCreateBudgetSuccess() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest(
                systemCategory.getId(),
                new BigDecimal("5000.00"),
                8,
                2026
        );

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.data.amount").value(5000.00))
                .andExpect(jsonPath("$.data.spentAmount").value(0.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(5000.00))
                .andExpect(jsonPath("$.data.thresholdStatus").value("NORMAL"));

        assertThat(budgetRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Create duplicate budget for same period returns 409 Conflict")
    void testCreateDuplicateBudgetReturns409() throws Exception {
        budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        CreateBudgetRequest request = new CreateBudgetRequest(
                systemCategory.getId(),
                new BigDecimal("6000.00"),
                8,
                2026
        );

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("A budget for this category and period already exists"));
    }

    @Test
    @DisplayName("Create budget with another user's category returns 403 Forbidden")
    void testCreateBudgetWithOtherUserCategoryForbidden() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest(
                userBCategory.getId(),
                new BigDecimal("1000.00"),
                8,
                2026
        );

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access to this category is forbidden"));
    }

    @Test
    @DisplayName("Budget spending calculation with transactions, income isolation, and threshold statuses")
    void testBudgetSpendingAndThresholds() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        // Create 2 EXPENSE transactions in August 2026 (2000 + 1500 = 3500 spent)
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("2000.00"), TransactionType.EXPENSE, "Groceries 1", LocalDate.of(2026, 8, 5)));
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("1500.00"), TransactionType.EXPENSE, "Groceries 2", LocalDate.of(2026, 8, 20)));

        // Create 1 INCOME transaction in August (should be completely ignored in spending)
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("10000.00"), TransactionType.INCOME, "Salary Bonus", LocalDate.of(2026, 8, 15)));

        // 3500 / 5000 = 70.0% -> NORMAL (< 80%)
        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(3500.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(1500.00))
                .andExpect(jsonPath("$.data.usagePercentage").value(70.0))
                .andExpect(jsonPath("$.data.thresholdStatus").value("NORMAL"));

        // Add 700 expense -> 4200 / 5000 = 84.0% -> WARNING (80% - 99.99%)
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("700.00"), TransactionType.EXPENSE, "Groceries 3", LocalDate.of(2026, 8, 25)));

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(4200.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(800.00))
                .andExpect(jsonPath("$.data.usagePercentage").value(84.0))
                .andExpect(jsonPath("$.data.thresholdStatus").value("WARNING"));

        // Add 1000 expense -> 5200 / 5000 = 104.0% -> EXCEEDED (>= 100%), remaining = -200
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("1000.00"), TransactionType.EXPENSE, "Groceries 4", LocalDate.of(2026, 8, 28)));

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(5200.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(-200.00))
                .andExpect(jsonPath("$.data.usagePercentage").value(104.0))
                .andExpect(jsonPath("$.data.thresholdStatus").value("EXCEEDED"));
    }

    @Test
    @DisplayName("Cross-user and cross-month spending isolation")
    void testCrossUserAndMonthSpendingIsolation() throws Exception {
        Budget budgetA = budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        // User A transaction in August 2026
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("2000.00"), TransactionType.EXPENSE, "User A Aug", LocalDate.of(2026, 8, 10)));

        // User B transaction in August 2026 (same category, different user)
        transactionRepository.save(new Transaction(userB.getId(), systemCategory.getId(),
                new BigDecimal("3000.00"), TransactionType.EXPENSE, "User B Aug", LocalDate.of(2026, 8, 10)));

        // User A transactions in July and September 2026 (same user, different month)
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("4000.00"), TransactionType.EXPENSE, "User A July", LocalDate.of(2026, 7, 15)));
        transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                new BigDecimal("6000.00"), TransactionType.EXPENSE, "User A Sept", LocalDate.of(2026, 9, 15)));

        // User A's August budget must show ONLY 2000.00 spent
        mockMvc.perform(get("/api/v1/budgets/" + budgetA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(2000.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(3000.00))
                .andExpect(jsonPath("$.data.usagePercentage").value(40.0));
    }

    @Test
    @DisplayName("List budgets with pagination and filters")
    void testListBudgetsWithFilters() throws Exception {
        // Create 2 budgets for User A and 1 for User B
        budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("1000.00"), 8, 2026));
        budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("2000.00"), 9, 2026));
        budgetRepository.save(new Budget(userB.getId(), systemCategory.getId(), new BigDecimal("3000.00"), 8, 2026));

        // User A lists budgets for August 2026
        mockMvc.perform(get("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("year", "2026")
                        .param("month", "8")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(1000.00))
                .andExpect(jsonPath("$.data.content[0].month").value(8));
    }

    @Test
    @DisplayName("Update own budget limit succeeds (200 OK)")
    void testUpdateOwnBudgetSuccess() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        UpdateBudgetRequest request = new UpdateBudgetRequest(new BigDecimal("7500.00"));

        mockMvc.perform(put("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(7500.00));
    }

    @Test
    @DisplayName("IDOR: Update another user's budget returns 404 Not Found")
    void testIDORUpdateOtherUserBudgetReturns404() throws Exception {
        Budget budgetB = budgetRepository.save(new Budget(userB.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        UpdateBudgetRequest request = new UpdateBudgetRequest(new BigDecimal("9999.00"));

        // User A attempts to update User B's budget
        mockMvc.perform(put("/api/v1/budgets/" + budgetB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found"));
    }

    @Test
    @DisplayName("Delete own budget succeeds (204 No Content)")
    void testDeleteOwnBudgetSuccess() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userA.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        mockMvc.perform(delete("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(budgetRepository.findById(budget.getId())).isEmpty();
    }

    @Test
    @DisplayName("IDOR: Delete another user's budget returns 404 Not Found")
    void testIDORDeleteOtherUserBudgetReturns404() throws Exception {
        Budget budgetB = budgetRepository.save(new Budget(userB.getId(), systemCategory.getId(), new BigDecimal("5000.00"), 8, 2026));

        // User A attempts to delete User B's budget
        mockMvc.perform(delete("/api/v1/budgets/" + budgetB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found"));

        assertThat(budgetRepository.findById(budgetB.getId())).isPresent();
    }

    @Test
    @DisplayName("Unauthenticated request to budgets returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
