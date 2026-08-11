package com.telemedecine.api.auth;

import com.telemedecine.api.dao.PasswordResetTokenRepository;
import com.telemedecine.api.dao.TokenRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.exception.EmailDeliveryException;
import com.telemedecine.api.exception.InvalidResetTokenException;
import com.telemedecine.api.model.token.PasswordResetToken;
import com.telemedecine.api.model.token.Token;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private PasswordResetService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository, resetTokenRepository, tokenRepository, passwordEncoder, emailService);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200/");
        ReflectionTestUtils.setField(service, "expirationMinutes", 20L);
        ReflectionTestUtils.setField(service, "requestCooldownSeconds", 60L);
        user = new Patient();
        user.setId(42L);
        user.setEmail("patient@example.com");
    }

    @Test
    void unknownEmailReturnsWithoutCreatingTokenOrSendingEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset(" Missing@example.com ");

        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(Long.class));
    }

    @Test
    void storesOnlyTokenHashAndEmailsRawTokenInAbsoluteUrl() throws Exception {
        when(userRepository.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());

        service.requestPasswordReset("PATIENT@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendPasswordResetEmail(eq("patient@example.com"), urlCaptor.capture(), eq(20L));

        String resetUrl = urlCaptor.getValue();
        String rawToken = resetUrl.substring(resetUrl.indexOf("token=") + 6);
        assertThat(resetUrl).startsWith("http://localhost:4200/reset-password?token=");
        assertThat(rawToken).hasSize(43);
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(rawToken));
        assertThat(tokenCaptor.getValue().getTokenHash()).doesNotContain(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt())
                .isEqualTo(tokenCaptor.getValue().getCreatedAt().plusSeconds(20 * 60));
    }

    @Test
    void rapidRepeatReturnsWithoutInvalidatingOrEmailingAgain() {
        PasswordResetToken recent = PasswordResetToken.builder()
                .createdAt(Instant.now().minusSeconds(20))
                .build();
        when(userRepository.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(recent));

        service.requestPasswordReset("patient@example.com");

        verify(resetTokenRepository, never()).markAllUnusedByUserAsUsed(any(), any());
        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(Long.class));
    }

    @Test
    void invalidExpiredAndUsedTokensUseTheSameSafeException() {
        ResetPasswordRequest request = new ResetPasswordRequest("a-valid-looking-reset-token", "newPassword", "newPassword");
        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(InvalidResetTokenException.class);

        PasswordResetToken expired = PasswordResetToken.builder()
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(InvalidResetTokenException.class);

        PasswordResetToken used = PasswordResetToken.builder()
                .expiresAt(Instant.now().plusSeconds(60))
                .usedAt(Instant.now())
                .build();
        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(used));
        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void rejectsPasswordMismatchBeforeUpdatingAnything() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "a-valid-looking-reset-token", "newPassword", "differentPassword");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match.");
        verify(resetTokenRepository, never()).findByTokenHash(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void successfulResetEncodesPasswordConsumesAllResetTokensAndRevokesSessions() {
        user.setMfaEnabled(true);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        Token accessToken = Token.builder().expired(false).revoked(false).build();
        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-password");
        when(tokenRepository.findAllValidTokensByUser(42L)).thenReturn(List.of(accessToken));

        service.resetPassword(new ResetPasswordRequest(
                "a-valid-looking-reset-token", "newPassword", "newPassword"));

        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getCredentialsUpdatedAt()).isNotNull();
        assertThat(user.getAuthenticationVersion()).isEqualTo(1);
        assertThat(user.isMfaEnabled()).isTrue();
        assertThat(accessToken.isExpired()).isTrue();
        assertThat(accessToken.isRevoked()).isTrue();
        verify(resetTokenRepository).markAllUnusedByUserAsUsed(eq(user), any(Instant.class));
        verify(tokenRepository).saveAll(List.of(accessToken));
    }

    @Test
    void invalidatesPreviousTokensBeforeSendingAndPropagatesSafeMailFailure() {
        when(userRepository.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());
        doThrow(new EmailDeliveryException(new RuntimeException("SMTP unavailable")))
                .when(emailService).sendPasswordResetEmail(eq(user.getEmail()), any(), eq(20L));

        assertThatThrownBy(() -> service.requestPasswordReset("patient@example.com"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageNotContaining("SMTP unavailable");

        InOrder order = inOrder(resetTokenRepository, emailService);
        order.verify(resetTokenRepository).markAllUnusedByUserAsUsed(eq(user), any());
        order.verify(resetTokenRepository).save(any());
        order.verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), any(), eq(20L));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
