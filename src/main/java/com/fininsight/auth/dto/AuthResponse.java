package com.fininsight.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Authentication response payload containing the JWT access token and user info.
 * Note: Refresh token is securely delivered via HttpOnly cookie, not in the body.
 */
@Schema(description = "Authentication response containing JWT bearer token and user summary")
public record AuthResponse(
        @Schema(description = "JWT Access Token for Authorization header", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "Token type scheme", example = "Bearer")
        String tokenType,

        @Schema(description = "Token expiration duration in milliseconds", example = "900000")
        long expiresIn,

        @Schema(description = "Authenticated user summary profile")
        UserSummaryResponse user
) {
    public static AuthResponse of(String accessToken, long expiresIn, UserSummaryResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}
