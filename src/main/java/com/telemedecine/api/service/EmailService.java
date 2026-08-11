package com.telemedecine.api.service;

public interface EmailService {
    void sendPasswordResetEmail(String recipient, String resetUrl, long expirationMinutes);
}
