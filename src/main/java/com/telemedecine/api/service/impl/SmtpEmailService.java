package com.telemedecine.api.service.impl;

import com.telemedecine.api.exception.EmailDeliveryException;
import com.telemedecine.api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendPasswordResetEmail(String recipient, String resetUrl, long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(recipient);
            helper.setSubject("MediLink — Reset your password");
            helper.setText(passwordResetHtml(resetUrl, expirationMinutes), true);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {

            Throwable root = exception;
            while (root.getCause() != null) {
                root = root.getCause();
            }

            log.error(
                    "Password reset email delivery failed. type={}, message={}, rootType={}, "
                            + "rootMessage={}, smtpHostConfiguredIncorrectly={}",
                    exception.getClass().getSimpleName(),
                    SmtpDiagnostics.sanitize(exception.getMessage()),
                    root.getClass().getSimpleName(),
                    SmtpDiagnostics.sanitize(root.getMessage()),
                    SmtpDiagnostics.isMalformedHost(mailProperties.getHost())
            );

            throw new EmailDeliveryException(exception);
        }
    }

    private String passwordResetHtml(String resetUrl, long expirationMinutes) {
        return """
                <!doctype html><html><body style="margin:0;background:#f3f7f9;font-family:Arial,sans-serif;color:#10213e">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:32px 16px;background:#f3f7f9">
                  <tr><td align="center"><table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#fff;border:1px solid #ddeaf1;border-radius:18px">
                    <tr><td style="padding:34px"><div style="font-size:24px;font-weight:700;color:#0796a8">MediLink</div>
                    <h1 style="margin:28px 0 12px;font-size:25px;color:#10213e">Reset your password</h1>
                    <p style="margin:0 0 24px;line-height:1.65;color:#61708a">We received a request to reset the password for your MediLink account.</p>
                    <a href="%s" style="display:inline-block;padding:13px 22px;border-radius:12px;background:#0796a8;color:#fff;font-weight:700;text-decoration:none">Reset password</a>
                    <p style="margin:24px 0 8px;line-height:1.6;color:#61708a">This link expires in %d minutes.</p>
                    <p style="margin:0;line-height:1.6;color:#61708a">If you did not request this change, you can safely ignore this email. For your security, never share this link with anyone.</p>
                    </td></tr></table></td></tr></table></body></html>
                """.formatted(resetUrl, expirationMinutes);
    }
}
