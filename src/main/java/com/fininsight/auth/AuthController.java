package com.fininsight.auth;

import com.fininsight.auth.dto.AuthResponse;
import com.fininsight.auth.dto.LoginRequest;
import com.fininsight.auth.dto.RegisterRequest;
import com.fininsight.auth.dto.UserSummaryResponse;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller providing user registration, login, token refresh, logout,
 * and current user profile endpoints.
 */
@Tag(name = "Authentication", description = "User authentication, JWT token refresh, and profile endpoints")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with BCrypt password hashing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserSummaryResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @Operation(summary = "Authenticate user", description = "Validates credentials, returns JWT access token, and sets HttpOnly refresh token cookie.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Bad credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @Operation(summary = "Refresh access token", description = "Uses HttpOnly refresh token cookie to rotate tokens and issue a fresh access token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "${application.security.jwt.cookie-name:refresh_token}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.refresh(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    @Operation(summary = "Logout user", description = "Revokes the active refresh token in database and clears the HttpOnly cookie.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "${application.security.jwt.cookie-name:refresh_token}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @Operation(
            summary = "Get current authenticated user",
            description = "Returns user profile information for the authenticated bearer token.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserSummaryResponse userSummary = authService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(userSummary));
    }
}
