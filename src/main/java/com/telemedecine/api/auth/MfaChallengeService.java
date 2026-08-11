package com.telemedecine.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MfaChallengeService {
    public enum Purpose { LOGIN, ENROLLMENT, ENROLLMENT_RESUME }

    public record Challenge(Long userId, Purpose purpose, Instant expiresAt, int failedAttempts) {}

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final long expirationMinutes;
    private final int maxAttempts;

    @Autowired
    public MfaChallengeService(
            @Value("${application.security.mfa.challenge-expiration-minutes:5}") long expirationMinutes,
            @Value("${application.security.mfa.max-attempts:5}") int maxAttempts
    ) {
        this(expirationMinutes, maxAttempts, new SecureRandom(), Clock.systemUTC());
    }

    MfaChallengeService(long expirationMinutes, int maxAttempts, SecureRandom secureRandom, Clock clock) {
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    public String create(Long userId, Purpose purpose) {
        removeExpired();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        challenges.put(hash(rawToken), new Challenge(
                userId,
                purpose,
                clock.instant().plus(expirationMinutes, ChronoUnit.MINUTES),
                0
        ));
        return rawToken;
    }

    public Challenge inspect(String rawToken) {
        String tokenHash = requireTokenHash(rawToken);
        Challenge challenge = challenges.get(tokenHash);
        if (challenge == null || !challenge.expiresAt().isAfter(clock.instant())) {
            challenges.remove(tokenHash);
            throw MfaVerificationException.expired();
        }
        return challenge;
    }

    public void recordFailure(String rawToken) {
        String tokenHash = requireTokenHash(rawToken);
        synchronized (challenges) {
            Challenge challenge = challenges.get(tokenHash);
            if (challenge == null || !challenge.expiresAt().isAfter(clock.instant())) {
                challenges.remove(tokenHash);
                throw MfaVerificationException.expired();
            }
            int attempts = challenge.failedAttempts() + 1;
            if (attempts >= maxAttempts) {
                challenges.remove(tokenHash);
                throw MfaVerificationException.tooManyAttempts();
            }
            challenges.put(tokenHash, new Challenge(
                    challenge.userId(), challenge.purpose(), challenge.expiresAt(), attempts));
        }
    }

    public Challenge consume(String rawToken) {
        String tokenHash = requireTokenHash(rawToken);
        Challenge challenge = challenges.remove(tokenHash);
        if (challenge == null || !challenge.expiresAt().isAfter(clock.instant())) {
            throw MfaVerificationException.expired();
        }
        return challenge;
    }

    private String requireTokenHash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw MfaVerificationException.expired();
        }
        return hash(rawToken);
    }

    private void removeExpired() {
        Instant now = clock.instant();
        challenges.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
