package com.telemedecine.api.service;

import com.telemedecine.api.dto.ai.AiChatRequest;
import com.telemedecine.api.dto.ai.AiChatResponse;

public interface AiPrediagnosticService {
    AiChatResponse chat(AiChatRequest request);
}
