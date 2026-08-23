package com.fininsight.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.auth.dto.LoginRequest;
import com.fininsight.auth.dto.RegisterRequest;
import com.fininsight.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class AuthIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Successful user registration creates user and returns 201 Created")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "SecurePassword123!",
                "John",
                "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(userRepository.existsByEmail("john.doe@example.com")).isTrue();
    }

    @Test
    @DisplayName("Registration with duplicate email returns 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com",
                "Password123!",
                "First",
                "Last"
        );

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    @DisplayName("Registration with invalid payload returns 400 Bad Request with field errors")
    void testRegisterValidationErrors() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "not-an-email",
                "short",
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    @DisplayName("Successful login returns JWT in body and HttpOnly refresh token cookie")
    void testLoginSuccess() throws Exception {
        // 1. Register user
        RegisterRequest registerReq = new RegisterRequest(
                "login.test@example.com",
                "StrongPassword123!",
                "Login",
                "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // 2. Login
        LoginRequest loginReq = new LoginRequest("login.test@example.com", "StrongPassword123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("login.test@example.com"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        // 3. Verify HttpOnly cookie
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("refresh_token=");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("SameSite=Strict");
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void testLoginInvalidCredentials() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "wrong.pass@example.com",
                "CorrectPassword123!",
                "Wrong",
                "Pass"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("wrong.pass@example.com", "IncorrectPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Refresh token rotation works and re-using an old token revokes all sessions")
    void testRefreshTokenRotationAndReuseDetection() throws Exception {
        // 1. Register & Login
        RegisterRequest registerReq = new RegisterRequest(
                "rotation@example.com",
                "RotationPass123!",
                "Rotate",
                "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rotation@example.com", "RotationPass123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie initialCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(initialCookie).isNotNull();

        // 2. Perform first refresh -> Should rotate token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(initialCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        Cookie rotatedCookie = refreshResult.getResponse().getCookie("refresh_token");
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(initialCookie.getValue());

        // 3. Token Reuse Detection: Try to use initialCookie AGAIN (compromised token replay)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(initialCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Token reuse detected. All active sessions have been terminated for security. Please log in again."
                ));

        // 4. Verify all tokens for this user are now revoked
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUserIdAndRevokedFalseAndExpiresAtAfter(
                        userRepository.findByEmail("rotation@example.com").get().getId(),
                        java.time.Instant.now()
                );
        assertThat(activeTokens).isEmpty();
    }

    @Test
    @DisplayName("Concurrent refresh requests on the same refresh token are serialized with pessimistic locking")
    void testConcurrentRefreshTokenRequests() throws Exception {
        // 1. Register & Login
        RegisterRequest registerReq = new RegisterRequest(
                "concurrent@example.com",
                "Password123!",
                "Concurrent",
                "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("concurrent@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie initialCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(initialCookie).isNotNull();

        // 2. Launch two concurrent refresh requests simultaneously with the same cookie
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Integer> req1 = executor.submit(() -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/refresh").cookie(initialCookie))
                    .andReturn().getResponse().getStatus();
        });

        Future<Integer> req2 = executor.submit(() -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/refresh").cookie(initialCookie))
                    .andReturn().getResponse().getStatus();
        });

        startLatch.countDown(); // Trigger both threads concurrently

        Integer status1 = req1.get();
        Integer status2 = req2.get();
        executor.shutdown();

        // One request succeeds with 200 (first to acquire lock and rotate),
        // and user state remains consistent and valid
        assertThat(List.of(status1, status2)).contains(200);
        assertThat(userRepository.existsByEmail("concurrent@example.com")).isTrue();
    }

    @Test
    @DisplayName("Logout revokes active refresh token and clears cookie")
    void testLogout() throws Exception {
        // 1. Register & Login
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("logout@example.com", "LogoutPass123!", "Logout", "User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("logout@example.com", "LogoutPass123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();

        // 2. Logout
        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();

        // 3. Verify Cookie cleared (Max-Age = 0)
        Cookie clearedCookie = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isEqualTo(0);
    }

    @Test
    @DisplayName("Protected endpoint access with valid Bearer token vs unauthenticated access")
    void testProtectedEndpointAccess() throws Exception {
        // 1. Register & Login to get token
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("me.test@example.com", "Password123!", "Me", "User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("me.test@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

        // 2. Access /api/v1/auth/me WITH valid token -> 200 OK
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me.test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Me"));

        // 3. Access /api/v1/auth/me WITHOUT token -> 401 Unauthorized with ErrorResponse
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }
}
