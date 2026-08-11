package com.telemedecine.api.exception;

public class CertificationUploadException extends RuntimeException {
    public CertificationUploadException(String message) { super(message); }
    public CertificationUploadException(String message, Throwable cause) { super(message, cause); }
}
