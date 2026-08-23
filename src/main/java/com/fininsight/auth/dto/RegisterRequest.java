package com.fininsight.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for user registration.
 */
@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Unique user email address", example = "user@fininsight.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Password (minimum 8 characters)", example = "Secret123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "First name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName
) {
}
