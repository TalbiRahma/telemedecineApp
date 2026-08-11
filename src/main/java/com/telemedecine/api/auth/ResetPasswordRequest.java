package com.telemedecine.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Password reset token is required")
        @Size(min = 20, max = 512, message = "Password reset token is invalid")
        String token,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
        String confirmPassword
) {
}
