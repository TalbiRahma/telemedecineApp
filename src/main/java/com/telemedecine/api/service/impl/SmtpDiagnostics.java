package com.telemedecine.api.service.impl;

import java.util.regex.Pattern;

public final class SmtpDiagnostics {
    private static final Pattern EMBEDDED_CONFIGURATION = Pattern.compile(
            "(?i)(SMTP_(HOST|PORT|USERNAME|PASSWORD)|FRONTEND_URL|PASSWORD_RESET_[A-Z_]+)\\s*=");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)(password|secret|token)\\s*[=:]\\s*[^\\s,;]+");
    private static final Pattern URI_CREDENTIALS = Pattern.compile(
            "(?i)([a-z][a-z0-9+.-]*://)[^\\s/@:]+:[^\\s/@]+@");

    private SmtpDiagnostics() {
    }

    public static boolean isMalformedHost(String host) {
        return host == null
                || host.isBlank()
                || host.chars().anyMatch(Character::isWhitespace)
                || host.contains("=")
                || host.toUpperCase().contains("SMTP_")
                || host.contains("FRONTEND_URL")
                || host.contains("PASSWORD_RESET_");
    }

    public static String sanitize(String message) {
        if (message == null) {
            return null;
        }
        if (EMBEDDED_CONFIGURATION.matcher(message).find()) {
            return "[redacted: embedded environment configuration]";
        }
        String withoutUriCredentials = URI_CREDENTIALS.matcher(message).replaceAll("$1<redacted>@");
        return SENSITIVE_ASSIGNMENT.matcher(withoutUriCredentials).replaceAll("$1=<redacted>");
    }
}
