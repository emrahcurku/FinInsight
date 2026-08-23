package com.fininsight.auth.dto;

import com.fininsight.user.Role;
import com.fininsight.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Safe user summary representation (no passwords or internal audit data).
 */
@Schema(description = "Safe public profile summary of a user")
public record UserSummaryResponse(
        @Schema(description = "User unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "User email address", example = "user@fininsight.com")
        String email,

        @Schema(description = "User first name", example = "John")
        String firstName,

        @Schema(description = "User last name", example = "Doe")
        String lastName,

        @Schema(description = "Assigned user authority role", example = "ROLE_USER")
        Role role
) {
    public static UserSummaryResponse fromUser(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
