package com.telemedecine.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiChatResponse(
        @JsonProperty("session_id") String sessionId,
        AiStructuredAnswer answer,
        String summary) {
}
