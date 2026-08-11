package com.telemedecine.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiStructuredAnswer(
        @JsonProperty("chief_summary") String chiefSummary,
        List<AiDifferentialItem> differential,
        String urgency,
        @JsonProperty("urgency_reason") String urgencyReason,
        @JsonProperty("next_steps") List<AiNextStep> nextSteps,
        @JsonProperty("red_flags") List<AiRedFlag> redFlags,
        AiDoctorRecommendation doctor,
        @JsonProperty("follow_up_questions") List<String> followUpQuestions,
        String disclaimer) {
}
