package com.telemedecine.api.auth;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MfaChallengeServiceTest {

    @Test
    void challengeIsBoundToUserAndSingleUse() {
        MfaChallengeService service = new MfaChallengeService(
                5, 5, new SecureRandom(), Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC));

        String token = service.create(42L, MfaChallengeService.Purpose.LOGIN);

        assertThat(service.inspect(token).userId()).isEqualTo(42L);
        assertThat(service.consume(token).purpose()).isEqualTo(MfaChallengeService.Purpose.LOGIN);
        assertThatThrownBy(() -> service.inspect(token))
                .isInstanceOf(MfaVerificationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void fifthFailedAttemptInvalidatesChallenge() {
        MfaChallengeService service = new MfaChallengeService(
                5, 5, new SecureRandom(), Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC));
        String token = service.create(42L, MfaChallengeService.Purpose.LOGIN);

        for (int attempt = 1; attempt < 5; attempt++) {
            service.recordFailure(token);
        }

        assertThatThrownBy(() -> service.recordFailure(token))
                .isInstanceOf(MfaVerificationException.class)
                .hasMessageContaining("Too many");
        assertThatThrownBy(() -> service.inspect(token))
                .isInstanceOf(MfaVerificationException.class);
    }

    @Test
    void expiredChallengeIsRejected() {
        MfaChallengeService service = new MfaChallengeService(
                0, 5, new SecureRandom(), Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC));
        String token = service.create(42L, MfaChallengeService.Purpose.LOGIN);

        assertThatThrownBy(() -> service.inspect(token))
                .isInstanceOf(MfaVerificationException.class)
                .hasMessageContaining("expired");
    }
}
