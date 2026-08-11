package com.telemedecine.api.auth;

public class MfaEnrollmentStateException extends RuntimeException {
    public MfaEnrollmentStateException(String message) {
        super(message);
    }
}
