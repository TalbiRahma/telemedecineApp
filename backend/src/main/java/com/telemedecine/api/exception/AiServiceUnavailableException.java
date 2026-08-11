package com.telemedecine.api.exception;

public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException() {
        super("The medical assistant is temporarily unavailable. Please try again.");
    }
}
