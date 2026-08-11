package com.telemedecine.api.auth;

public class MfaVerificationException extends RuntimeException {
    public enum Reason { INVALID_CODE, EXPIRED_CHALLENGE, TOO_MANY_ATTEMPTS }

    private final Reason reason;

    private MfaVerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static MfaVerificationException invalidCode() {
        return new MfaVerificationException(Reason.INVALID_CODE,
                "Invalid or expired verification code.");
    }

    public static MfaVerificationException expired() {
        return new MfaVerificationException(Reason.EXPIRED_CHALLENGE,
                "Your verification session has expired. Please sign in again.");
    }

    public static MfaVerificationException tooManyAttempts() {
        return new MfaVerificationException(Reason.TOO_MANY_ATTEMPTS,
                "Too many verification attempts. Please sign in again.");
    }

    public Reason getReason() {
        return reason;
    }
}
