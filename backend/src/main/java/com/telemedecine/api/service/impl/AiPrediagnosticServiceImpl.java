package com.telemedecine.api.service.impl;

import com.telemedecine.api.dto.ai.AiChatRequest;
import com.telemedecine.api.dto.ai.AiChatResponse;
import com.telemedecine.api.exception.AiServiceUnavailableException;
import com.telemedecine.api.service.AiPrediagnosticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class AiPrediagnosticServiceImpl implements AiPrediagnosticService {
    private final RestClient aiRestClient;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        try {
            AiChatResponse response = aiRestClient.post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .body(AiChatResponse.class);
            if (response == null || response.answer() == null || response.sessionId() == null) {
                throw new AiServiceUnavailableException();
            }
            return response;
        } catch (AiServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiServiceUnavailableException();
        }
    }
}
