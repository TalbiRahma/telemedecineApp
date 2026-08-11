package com.telemedecine.api.auth;

import jakarta.validation.constraints.NotBlank;

public record MfaEnrollmentRequest(@NotBlank String challengeToken) {
}
