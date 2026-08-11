package com.telemedecine.api.config;

import com.telemedecine.api.service.impl.SmtpDiagnostics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailConfigurationValidator implements ApplicationRunner {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MailProperties mailProperties;

    @Override
    public void run(ApplicationArguments arguments) {
        String host = mailProperties.getHost();
        if (SmtpDiagnostics.isMalformedHost(host)) {
            throw invalid("spring.mail.host must contain only a hostname");
        }

        Integer port = mailProperties.getPort();
        if (port == null || port < 1 || port > 65535) {
            throw invalid("spring.mail.port must be between 1 and 65535");
        }

        String username = mailProperties.getUsername();
        if (username == null || !EMAIL.matcher(username).matches()) {
            throw invalid("spring.mail.username must be a valid email address");
        }

        String password = mailProperties.getPassword();
        if (password == null || password.isBlank()) {
            throw invalid("spring.mail.password must be configured");
        }

        log.info("SMTP configuration: host={}, port={}, username={}, passwordConfigured=true",
                host, port, maskEmail(username));
    }

    private IllegalStateException invalid(String detail) {
        return new IllegalStateException("Invalid SMTP configuration: " + detail + ".");
    }

    private String maskEmail(String username) {
        int at = username.indexOf('@');
        String localPart = username.substring(0, at);
        int visibleLength = Math.min(2, localPart.length());
        return localPart.substring(0, visibleLength) + "***" + username.substring(at);
    }
}
