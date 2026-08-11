package com.telemedecine.api.controller;

import com.telemedecine.api.auth.AuthenticationService;
import com.telemedecine.api.dto.ai.AiChatResponse;
import com.telemedecine.api.dto.ai.AiDoctorRecommendation;
import com.telemedecine.api.dto.ai.AiStructuredAnswer;
import com.telemedecine.api.exception.AiServiceUnavailableException;
import com.telemedecine.api.security.JwtAuthenticationFilter;
import com.telemedecine.api.service.AiPrediagnosticService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AiPrediagnosticController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import({AiPrediagnosticControllerSecurityTest.TestSecurityConfiguration.class, ApiExceptionHandler.class})
class AiPrediagnosticControllerSecurityTest {
    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @MockBean
    private AiPrediagnosticService aiService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCanUseChat() throws Exception {
        AiStructuredAnswer answer = new AiStructuredAnswer(
                "Fever for two days", List.of(), "MEDIUM", "Persistent high fever",
                List.of(), List.of(), new AiDoctorRecommendation("GENERAL_MEDICINE", "today"),
                List.of("What is your temperature?"), "DISCLAIMER: Informational only.");
        when(aiService.chat(any())).thenReturn(new AiChatResponse("session-1", answer, "Fever"));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"session_id\":\"session-1\",\"message\":\"I have a fever\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value("session-1"))
                .andExpect(jsonPath("$.answer.urgency").value("MEDIUM"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorIsForbidden() throws Exception {
        performValidRequest().andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminIsForbidden() throws Exception {
        performValidRequest().andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        performValidRequest().andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void invalidMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"session_id\":\"../invalid\",\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AI_REQUEST"));
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void aiFailureReturnsCleanServiceUnavailableResponse() throws Exception {
        when(aiService.chat(any())).thenThrow(new AiServiceUnavailableException());
        performValidRequest()
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "The medical assistant is temporarily unavailable. Please try again."));
    }

    private org.springframework.test.web.servlet.ResultActions performValidRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"session_id\":\"session-1\",\"message\":\"I have a fever\"}"));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfiguration {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(basic -> {})
                    .build();
        }
    }
}
