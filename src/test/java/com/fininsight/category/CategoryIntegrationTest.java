package com.fininsight.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.auth.JwtTokenProvider;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.dto.CreateCategoryRequest;
import com.fininsight.category.dto.UpdateCategoryRequest;
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
public class CategoryIntegrationTest {

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
        private String tokenA;
        private Category systemCategory;
        private Category userACategory;
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

                User userB = userRepository.save(new User("user.b@example.com", "passB", "User", "B", Role.ROLE_USER));

                // 2. Create Categories
                systemCategory = categoryRepository.save(new Category("Salary", CategoryType.INCOME));
                userACategory = categoryRepository
                                .save(new Category(userA.getId(), "User A Custom Cat", CategoryType.EXPENSE));
                userBCategory = categoryRepository
                                .save(new Category(userB.getId(), "User B Secret Cat", CategoryType.EXPENSE));
        }

        @Test
        @DisplayName("List categories returns system and user A's categories only")
        void testListCategoriesIsolation() throws Exception {
                mockMvc.perform(get("/api/v1/categories")
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[?(@.name == 'Salary')]").exists())
                                .andExpect(jsonPath("$.data[?(@.name == 'User A Custom Cat')]").exists())
                                .andExpect(jsonPath("$.data[?(@.name == 'User B Secret Cat')]").doesNotExist());
        }

        @Test
        @DisplayName("Create custom category succeeds (201 Created)")
        void testCreateCategorySuccess() throws Exception {
                CreateCategoryRequest request = new CreateCategoryRequest("Freelancing", CategoryType.INCOME);

                mockMvc.perform(post("/api/v1/categories")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.name").value("Freelancing"))
                                .andExpect(jsonPath("$.data.system").value(false));

                assertThat(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userA.getId(), "Freelancing",
                                CategoryType.INCOME))
                                .isTrue();
        }

        @Test
        @DisplayName("Create duplicate category returns 409 Conflict")
        void testCreateDuplicateCategoryReturns409() throws Exception {
                CreateCategoryRequest request = new CreateCategoryRequest("Salary", CategoryType.INCOME);

                mockMvc.perform(post("/api/v1/categories")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("A category with this name and type already exists"));
        }

        @Test
        @DisplayName("Update own custom category succeeds (200 OK)")
        void testUpdateOwnCategorySuccess() throws Exception {
                UpdateCategoryRequest request = new UpdateCategoryRequest("Updated Cat Name");

                mockMvc.perform(put("/api/v1/categories/" + userACategory.getId())
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Updated Cat Name"));
        }

        @Test
        @DisplayName("Update system category returns 403 Forbidden")
        void testUpdateSystemCategoryForbidden() throws Exception {
                UpdateCategoryRequest request = new UpdateCategoryRequest("Hacked Salary");

                mockMvc.perform(put("/api/v1/categories/" + systemCategory.getId())
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("System categories cannot be modified"));
        }

        @Test
        @DisplayName("IDOR: Update another user's category returns 404 Not Found")
        void testIDORUpdateOtherUserCategoryReturns404() throws Exception {
                UpdateCategoryRequest request = new UpdateCategoryRequest("Hacked Cat");

                mockMvc.perform(put("/api/v1/categories/" + userBCategory.getId())
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Category not found"));
        }

        @Test
        @DisplayName("Delete own category succeeds when not in use (204 No Content)")
        void testDeleteOwnCategorySuccess() throws Exception {
                mockMvc.perform(delete("/api/v1/categories/" + userACategory.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isNoContent());

                assertThat(categoryRepository.findById(userACategory.getId())).isEmpty();
        }

        @Test
        @DisplayName("Delete system category returns 403 Forbidden")
        void testDeleteSystemCategoryForbidden() throws Exception {
                mockMvc.perform(delete("/api/v1/categories/" + systemCategory.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("System categories cannot be deleted"));
        }

        @Test
        @DisplayName("IDOR: Delete another user's category returns 404 Not Found")
        void testIDORDeleteOtherUserCategoryReturns404() throws Exception {
                mockMvc.perform(delete("/api/v1/categories/" + userBCategory.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Category not found"));

                assertThat(categoryRepository.findById(userBCategory.getId())).isPresent();
        }

        @Test
        @DisplayName("Delete category currently in use by transactions returns 409 Conflict")
        void testDeleteCategoryInUseByTransactionReturns409() throws Exception {
                // Create transaction referencing userACategory
                transactionRepository.save(new Transaction(
                                userA.getId(),
                                userACategory.getId(),
                                new BigDecimal("50.00"),
                                TransactionType.EXPENSE,
                                "Coffee",
                                LocalDate.now()));

                mockMvc.perform(delete("/api/v1/categories/" + userACategory.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value(
                                                "Category is currently in use by transactions and cannot be deleted"));
        }

        @Test
        @DisplayName("Delete category currently in use by budgets returns 409 Conflict")
        void testDeleteCategoryInUseByBudgetReturns409() throws Exception {
                // Create budget referencing userACategory
                budgetRepository.save(new Budget(
                                userA.getId(),
                                userACategory.getId(),
                                new BigDecimal("500.00"),
                                (short) 8,
                                (short) 2026));

                mockMvc.perform(delete("/api/v1/categories/" + userACategory.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value(
                                                "Category is currently in use by budgets and cannot be deleted"));
        }

        @Test
        @DisplayName("Unauthenticated request to categories returns 401 Unauthorized")
        void testUnauthenticatedAccessReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/categories"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }
}
