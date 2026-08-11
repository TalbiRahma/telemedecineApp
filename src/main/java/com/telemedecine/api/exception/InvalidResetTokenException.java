package com.telemedecine.api.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Password reset link is invalid or expired.");
    }
}
