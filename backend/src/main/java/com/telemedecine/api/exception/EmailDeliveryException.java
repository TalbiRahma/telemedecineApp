package com.telemedecine.api.exception;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(Throwable cause) {
        super("Password reset email could not be delivered.", cause);
    }
}
