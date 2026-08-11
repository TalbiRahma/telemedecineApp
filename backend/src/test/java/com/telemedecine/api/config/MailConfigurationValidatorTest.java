package com.telemedecine.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailConfigurationValidatorTest {
    @Test
    void acceptsCompleteSmtpConfiguration() {
        MailConfigurationValidator validator = validator("smtp.gmail.com", 587,
                "mailer@example.com", "app-password");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEnvironmentBlockStoredAsHostWithoutEchoingIt() {
        String malformedHost = "smtp.gmail.com SMTP_PORT=587 SMTP_USERNAME=mailer@example.com "
                + "SMTP_PASSWORD=do-not-log FRONTEND_URL=http://localhost:4200";
        MailConfigurationValidator validator = validator(malformedHost, 587,
                "mailer@example.com", "app-password");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid SMTP configuration: spring.mail.host must contain only a hostname.")
                .hasMessageNotContaining("do-not-log")
                .hasMessageNotContaining("SMTP_PASSWORD");
    }

    @Test
    void rejectsMissingCredentials() {
        assertThatThrownBy(() -> validator("smtp.gmail.com", 587, "", "app-password").run(null))
                .hasMessage("Invalid SMTP configuration: spring.mail.username must be a valid email address.");
        assertThatThrownBy(() -> validator("smtp.gmail.com", 587, "mailer@example.com", "").run(null))
                .hasMessage("Invalid SMTP configuration: spring.mail.password must be configured.");
    }

    private MailConfigurationValidator validator(String host, int port, String username, String password) {
        MailProperties properties = new MailProperties();
        properties.setHost(host);
        properties.setPort(port);
        properties.setUsername(username);
        properties.setPassword(password);
        return new MailConfigurationValidator(properties);
    }
}
