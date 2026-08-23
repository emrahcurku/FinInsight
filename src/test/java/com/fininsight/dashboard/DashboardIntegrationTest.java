package com.fininsight.dashboard;

import com.fininsight.auth.JwtTokenProvider;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    private String tokenA;

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

        // 1. Users
        User userA = userRepository.save(new User("user.a@example.com", "passA", "User", "A", Role.ROLE_USER));
        tokenA = jwtTokenProvider.generateAccessToken(userA.getId(), userA.getEmail(), userA.getRole());

        User userB = userRepository.save(new User("user.b@example.com", "passB", "User", "B", Role.ROLE_USER));

        // 2. Categories
        Category systemIncomeCat = categoryRepository.save(new Category("Salary", CategoryType.INCOME));
        Category systemExpenseCat = categoryRepository.save(new Category("Groceries", CategoryType.EXPENSE));
        Category userACustomCat = categoryRepository.save(new Category(userA.getId(), "Dining", CategoryType.EXPENSE));

        // 3. User A Transactions (August 2026)
        transactionRepository.save(new Transaction(userA.getId(), systemIncomeCat.getId(),
                new BigDecimal("10000.00"), TransactionType.INCOME, "Monthly Salary", LocalDate.of(2026, 8, 1)));
        transactionRepository.save(new Transaction(userA.getId(), systemExpenseCat.getId(),
                new BigDecimal("3000.00"), TransactionType.EXPENSE, "Supermarket", LocalDate.of(2026, 8, 5)));
        transactionRepository.save(new Transaction(userA.getId(), userACustomCat.getId(),
                new BigDecimal("1000.00"), TransactionType.EXPENSE, "Restaurant", LocalDate.of(2026, 8, 10)));

        // 4. User B Transactions (Should be completely isolated)
        transactionRepository.save(new Transaction(userB.getId(), systemExpenseCat.getId(),
                new BigDecimal("5000.00"), TransactionType.EXPENSE, "User B Groceries", LocalDate.of(2026, 8, 5)));

        // 5. Budgets
        budgetRepository.save(new Budget(userA.getId(), systemExpenseCat.getId(), new BigDecimal("5000.00"), 8, 2026));
        budgetRepository.save(new Budget(userA.getId(), userACustomCat.getId(), new BigDecimal("1500.00"), 8, 2026));
        budgetRepository.save(new Budget(userB.getId(), systemExpenseCat.getId(), new BigDecimal("9000.00"), 8, 2026));
    }

    @Test
    @DisplayName("Dashboard returns full composite view for authenticated user and isolates other users")
    void testDashboardAuthenticatedSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Summary
                .andExpect(jsonPath("$.data.financialSummary.totalIncome").value(10000.00))
                .andExpect(jsonPath("$.data.financialSummary.totalExpense").value(4000.00))
                .andExpect(jsonPath("$.data.financialSummary.netBalance").value(6000.00))
                .andExpect(jsonPath("$.data.financialSummary.transactionCount").value(3))
                // Monthly trend (6 continuous entries)
                .andExpect(jsonPath("$.data.monthlyTrend.length()").value(6))
                // Category spending (Top 5)
                .andExpect(jsonPath("$.data.categorySpending[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.data.categorySpending[0].amount").value(3000.00))
                .andExpect(jsonPath("$.data.categorySpending[0].percentage").value(75.0))
                .andExpect(jsonPath("$.data.categorySpending[1].categoryName").value("Dining"))
                .andExpect(jsonPath("$.data.categorySpending[1].amount").value(1000.00))
                .andExpect(jsonPath("$.data.categorySpending[1].percentage").value(25.0))
                // Budget Overview
                .andExpect(jsonPath("$.data.budgetOverview.totalBudget").value(6500.00))
                .andExpect(jsonPath("$.data.budgetOverview.totalSpent").value(4000.00))
                .andExpect(jsonPath("$.data.budgetOverview.totalRemaining").value(2500.00))
                .andExpect(jsonPath("$.data.budgetOverview.overallUsagePercentage").value(61.538))
                .andExpect(jsonPath("$.data.budgetOverview.normalBudgetCount").value(2))
                // Recent Transactions (limit 5)
                .andExpect(jsonPath("$.data.recentTransactions.length()").value(3))
                // Previous Month Comparison
                .andExpect(jsonPath("$.data.previousMonthComparison.currentMonthExpense").value(4000.00))
                .andExpect(jsonPath("$.data.previousMonthComparison.trend").value("INCREASED"))
                // Insights
                .andExpect(jsonPath("$.data.insights").isArray());
    }

    @Test
    @DisplayName("Invalid date range where from > to returns 400 Bad Request")
    void testDashboardInvalidDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-08-31")
                        .param("to", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid date range: 'from' date must be on or before 'to' date"));
    }

    @Test
    @DisplayName("Unauthenticated request to dashboard returns 401 Unauthorized")
    void testDashboardUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
