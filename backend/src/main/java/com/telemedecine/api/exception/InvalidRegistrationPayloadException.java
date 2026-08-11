package com.telemedecine.api.exception;

public class InvalidRegistrationPayloadException extends RuntimeException {
    public InvalidRegistrationPayloadException(String message, Throwable cause) { super(message, cause); }
}
