package com.telemedecine.api.auth;

import com.telemedecine.api.dao.PasswordResetTokenRepository;
import com.telemedecine.api.dao.TokenRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.exception.InvalidResetTokenException;
import com.telemedecine.api.model.token.PasswordResetToken;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${password-reset.token-expiration-minutes:20}")
    private long expirationMinutes;

    @Value("${password-reset.request-cooldown-seconds:60}")
    private long requestCooldownSeconds;

    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }

        Instant now = Instant.now();
        boolean coolingDown = resetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(token -> token.getCreatedAt().plusSeconds(requestCooldownSeconds).isAfter(now))
                .orElse(false);
        if (coolingDown) {
            return;
        }

        resetTokenRepository.markAllUnusedByUserAsUsed(user, now);
        String rawToken = generateToken();
        resetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .build());

        String baseUrl = frontendUrl.replaceAll("/+$", "");
        String resetUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl, expirationMinutes);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(InvalidResetTokenException::new);
        Instant now = Instant.now();
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw new InvalidResetTokenException();
        }

        UserEntity user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCredentialsUpdatedAt(now.truncatedTo(ChronoUnit.SECONDS));
        int currentVersion = user.getAuthenticationVersion() == null ? 0 : user.getAuthenticationVersion();
        user.setAuthenticationVersion(currentVersion + 1);
        userRepository.save(user);
        resetTokenRepository.markAllUnusedByUserAsUsed(user, now);

        var validTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        validTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
