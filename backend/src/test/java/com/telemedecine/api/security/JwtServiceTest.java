package com.telemedecine.api.security;

import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;
    private Patient user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120_000L);

        user = new Patient();
        user.setId(7L);
        user.setEmail("patient@example.com");
        user.setPassword("encoded");
        user.setRole(Role.PATIENT);
    }

    @Test
    void passwordResetVersionInvalidatesPreviouslyIssuedAccessAndRefreshTokens() {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        assertThat(jwtService.isTokenValid(accessToken, user)).isTrue();
        assertThat(jwtService.isTokenValid(refreshToken, user)).isTrue();
        assertThat(jwtService.isAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.isAccessToken(refreshToken)).isFalse();

        user.setAuthenticationVersion(1);

        assertThat(jwtService.isTokenValid(accessToken, user)).isFalse();
        assertThat(jwtService.isTokenValid(refreshToken, user)).isFalse();
        assertThat(jwtService.isTokenValid(jwtService.generateToken(user), user)).isTrue();
    }

    @Test
    void adminAccessTokenContainsAdminRole() {
        Admin admin = new Admin();
        admin.setId(8L);
        admin.setEmail("admin@example.com");
        admin.setPassword("encoded");
        admin.setRole(Role.ADMIN);

        String accessToken = jwtService.generateToken(admin);
        String tokenRole = jwtService.extractClaim(accessToken,
                claims -> claims.get("role", String.class));

        assertThat(tokenRole).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(accessToken, admin)).isTrue();
    }
}
