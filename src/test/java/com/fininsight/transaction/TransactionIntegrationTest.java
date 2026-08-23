package com.fininsight.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.auth.JwtTokenProvider;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.transaction.dto.CreateTransactionRequest;
import com.fininsight.transaction.dto.UpdateTransactionRequest;
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
public class TransactionIntegrationTest {

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
        private JwtTokenProvider jwtTokenProvider;

        private User userA;
        private User userB;
        private String tokenA;
        private String tokenB;
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
                categoryRepository.deleteAll();
                userRepository.deleteAll();

                // 1. Create User A
                userA = userRepository.save(new User(
                                "user.a@example.com",
                                "encodedPassA",
                                "User",
                                "A",
                                Role.ROLE_USER));
                tokenA = jwtTokenProvider.generateAccessToken(userA.getId(), userA.getEmail(), userA.getRole());

                // 2. Create User B
                userB = userRepository.save(new User(
                                "user.b@example.com",
                                "encodedPassB",
                                "User",
                                "B",
                                Role.ROLE_USER));
                tokenB = jwtTokenProvider.generateAccessToken(userB.getId(), userB.getEmail(), userB.getRole());

                // 3. Create Categories
                systemCategory = categoryRepository.save(new Category("Salary", CategoryType.INCOME));
                userACategory = categoryRepository
                                .save(new Category(userA.getId(), "User A Custom Cat", CategoryType.EXPENSE));
                userBCategory = categoryRepository
                                .save(new Category(userB.getId(), "User B Secret Cat", CategoryType.EXPENSE));
        }

        @Test
        @DisplayName("Create transaction with system category succeeds (201 Created)")
        void testCreateTransactionWithSystemCategory() throws Exception {
                CreateTransactionRequest request = new CreateTransactionRequest(
                                systemCategory.getId(),
                                new BigDecimal("5000.00"),
                                TransactionType.INCOME,
                                "Monthly Salary",
                                LocalDate.now());

                mockMvc.perform(post("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.categoryName").value("Salary"))
                                .andExpect(jsonPath("$.data.amount").value(5000.00))
                                .andExpect(jsonPath("$.data.type").value("INCOME"));

                assertThat(transactionRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Create transaction with own category succeeds (201 Created)")
        void testCreateTransactionWithOwnCategory() throws Exception {
                CreateTransactionRequest request = new CreateTransactionRequest(
                                userACategory.getId(),
                                new BigDecimal("120.50"),
                                TransactionType.EXPENSE,
                                "Personal expenses",
                                LocalDate.now());

                mockMvc.perform(post("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.categoryName").value("User A Custom Cat"));
        }

        @Test
        @DisplayName("Create transaction with another user's category returns 403 Forbidden")
        void testCreateTransactionWithAnotherUserCategoryForbidden() throws Exception {
                CreateTransactionRequest request = new CreateTransactionRequest(
                                userBCategory.getId(),
                                new BigDecimal("75.00"),
                                TransactionType.EXPENSE,
                                "Unauthorized attempt",
                                LocalDate.now());

                mockMvc.perform(post("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Access to this category is forbidden"));
        }

        @Test
        @DisplayName("Create transaction with invalid amount returns 400 Bad Request")
        void testCreateTransactionValidationErrors() throws Exception {
                CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                                systemCategory.getId(),
                                new BigDecimal("-10.00"), // Invalid negative amount
                                null, // Missing type
                                "Invalid transaction",
                                null // Missing date
                );

                mockMvc.perform(post("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors.amount").exists())
                                .andExpect(jsonPath("$.errors.type").exists())
                                .andExpect(jsonPath("$.errors.transactionDate").exists());
        }

        @Test
        @DisplayName("Get own transaction by ID returns 200 OK")
        void testGetOwnTransactionSuccess() throws Exception {
                Transaction tx = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("250.00"),
                                TransactionType.EXPENSE,
                                "Utility bill",
                                LocalDate.now()));

                mockMvc.perform(get("/api/v1/transactions/" + tx.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(tx.getId().toString()))
                                .andExpect(jsonPath("$.data.categoryName").value("Salary"));
        }

        @Test
        @DisplayName("IDOR: Get another user's transaction returns 404 Not Found")
        void testIDORGetAnotherUserTransactionReturns404() throws Exception {
                Transaction userATx = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("999.00"),
                                TransactionType.EXPENSE,
                                "Private payment",
                                LocalDate.now()));

                // User B tries to view User A's transaction
                mockMvc.perform(get("/api/v1/transactions/" + userATx.getId())
                                .header("Authorization", "Bearer " + tokenB))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Transaction not found"));
        }

        @Test
        @DisplayName("List transactions with filters and pagination enforces user boundary")
        void testListTransactionsWithFiltersAndUserIsolation() throws Exception {
                // Create 3 transactions for User A
                transactionRepository.save(new Transaction(userA.getId(), systemCategory.getId(),
                                new BigDecimal("1000.00"), TransactionType.INCOME, "Jan Bonus",
                                LocalDate.of(2026, 1, 15)));
                transactionRepository.save(new Transaction(userA.getId(), userACategory.getId(),
                                new BigDecimal("50.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 2, 10)));
                transactionRepository.save(new Transaction(userA.getId(), userACategory.getId(),
                                new BigDecimal("150.00"), TransactionType.EXPENSE, "Dinner",
                                LocalDate.of(2026, 2, 20)));

                // Create 1 transaction for User B
                transactionRepository.save(new Transaction(userB.getId(), userBCategory.getId(),
                                new BigDecimal("500.00"), TransactionType.EXPENSE, "Secret Tx",
                                LocalDate.of(2026, 2, 10)));

                // User A lists EXPENSE transactions in February
                mockMvc.perform(get("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenA)
                                .param("from", "2026-02-01")
                                .param("to", "2026-02-28")
                                .param("type", "EXPENSE")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(2))
                                .andExpect(jsonPath("$.data.content[0].description").value("Dinner"))
                                .andExpect(jsonPath("$.data.content[1].description").value("Coffee"));

                // User B lists transactions -> only sees their own 1 transaction
                mockMvc.perform(get("/api/v1/transactions")
                                .header("Authorization", "Bearer " + tokenB))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].description").value("Secret Tx"));
        }

        @Test
        @DisplayName("Update own transaction succeeds (200 OK)")
        void testUpdateOwnTransactionSuccess() throws Exception {
                Transaction tx = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("100.00"),
                                TransactionType.EXPENSE,
                                "Old Description",
                                LocalDate.now()));

                UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                                userACategory.getId(),
                                new BigDecimal("175.00"),
                                TransactionType.EXPENSE,
                                "Updated Description",
                                LocalDate.now());

                mockMvc.perform(put("/api/v1/transactions/" + tx.getId())
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.amount").value(175.00))
                                .andExpect(jsonPath("$.data.description").value("Updated Description"))
                                .andExpect(jsonPath("$.data.categoryName").value("User A Custom Cat"));
        }

        @Test
        @DisplayName("IDOR: Update another user's transaction returns 404 Not Found")
        void testIDORUpdateAnotherUserTransactionReturns404() throws Exception {
                Transaction txA = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("100.00"),
                                TransactionType.EXPENSE,
                                "User A Tx",
                                LocalDate.now()));

                UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                                systemCategory.getId(),
                                new BigDecimal("999.00"),
                                TransactionType.EXPENSE,
                                "Hacked Desc",
                                LocalDate.now());

                // User B attempts to update User A's transaction
                mockMvc.perform(put("/api/v1/transactions/" + txA.getId())
                                .header("Authorization", "Bearer " + tokenB)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateReq)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Transaction not found"));
        }

        @Test
        @DisplayName("Delete own transaction returns 204 No Content")
        void testDeleteOwnTransactionSuccess() throws Exception {
                Transaction tx = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("80.00"),
                                TransactionType.EXPENSE,
                                "To Delete",
                                LocalDate.now()));

                mockMvc.perform(delete("/api/v1/transactions/" + tx.getId())
                                .header("Authorization", "Bearer " + tokenA))
                                .andExpect(status().isNoContent());

                assertThat(transactionRepository.findById(tx.getId())).isEmpty();
        }

        @Test
        @DisplayName("IDOR: Delete another user's transaction returns 404 Not Found")
        void testIDORDeleteAnotherUserTransactionReturns404() throws Exception {
                Transaction txA = transactionRepository.save(new Transaction(
                                userA.getId(),
                                systemCategory.getId(),
                                new BigDecimal("80.00"),
                                TransactionType.EXPENSE,
                                "User A Tx",
                                LocalDate.now()));

                // User B attempts to delete User A's transaction
                mockMvc.perform(delete("/api/v1/transactions/" + txA.getId())
                                .header("Authorization", "Bearer " + tokenB))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Transaction not found"));

                // Verify User A's transaction is still in database
                assertThat(transactionRepository.findById(txA.getId())).isPresent();
        }

        @Test
        @DisplayName("Unauthenticated request to transaction endpoints returns 401 Unauthorized")
        void testUnauthenticatedAccessReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/transactions"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }
}
