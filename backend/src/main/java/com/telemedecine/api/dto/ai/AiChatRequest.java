package com.telemedecine.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @JsonProperty("session_id")
        @NotBlank(message = "Session ID is required.")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Session ID is invalid.")
        String sessionId,
        @NotBlank(message = "Medical message is required.")
        @Size(max = 2000, message = "Medical message must not exceed 2000 characters.")
        String message) {
}
