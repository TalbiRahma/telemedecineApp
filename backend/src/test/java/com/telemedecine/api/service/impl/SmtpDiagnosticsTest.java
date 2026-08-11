package com.telemedecine.api.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpDiagnosticsTest {
    @Test
    void redactsEntireMessageContainingEmbeddedEnvironmentConfiguration() {
        String message = "Unknown host smtp.gmail.com SMTP_PORT=587 SMTP_PASSWORD=secret-value";

        assertThat(SmtpDiagnostics.sanitize(message))
                .isEqualTo("[redacted: embedded environment configuration]")
                .doesNotContain("secret-value");
    }

    @Test
    void recognizesMalformedHost() {
        assertThat(SmtpDiagnostics.isMalformedHost("smtp.gmail.com")).isFalse();
        assertThat(SmtpDiagnostics.isMalformedHost("smtp.gmail.com SMTP_PORT=587")).isTrue();
        assertThat(SmtpDiagnostics.isMalformedHost("smtp.gmail.com\nSMTP_USERNAME=user@example.com")).isTrue();
    }
}
