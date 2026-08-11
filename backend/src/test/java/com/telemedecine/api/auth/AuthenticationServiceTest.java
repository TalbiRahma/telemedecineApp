package com.telemedecine.api.auth;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.dao.TokenRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.mapper.SpecialtyMapper;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.token.Token;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.security.JwtService;
import com.telemedecine.api.security.tfa.TwoFactorAuthenticationService;
import com.telemedecine.api.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TwoFactorAuthenticationService tfaService;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private SpecialtyMapper specialtyMapper;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private PatientRepository patientRepository;
    @Mock private MfaChallengeService mfaChallengeService;

    @InjectMocks private AuthenticationService service;

    @Test
    void patientRegistrationWithMfaCreatesPendingEnrollmentWithoutJwt() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Patient").lastname("Example").email("patient@example.com")
                .password("validPassword").mfaEnabled(true).build();
        when(passwordEncoder.encode("validPassword")).thenReturn("encoded-password");
        when(tfaService.generateNewSecret()).thenReturn("secret");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(9L);
            return patient;
        });
        when(tfaService.generateQrCodeImageUri("secret")).thenReturn("data:image/png;base64,qr");
        when(mfaChallengeService.create(9L, MfaChallengeService.Purpose.ENROLLMENT))
                .thenReturn("enrollment-challenge");

        AuthenticationResponse response = service.register(request);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        assertThat(patientCaptor.getValue().isMfaEnabled()).isFalse();
        assertThat(patientCaptor.getValue().isMfaEnrollmentPending()).isTrue();
        assertThat(response.isMfaEnrollmentRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("enrollment-challenge");
        assertThat(response.getQrCodeImageUri()).startsWith("data:image/png");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void mfaLoginChecksPasswordThenReturnsChallengeWithoutTokensOrQrCode() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .mfaEnrollmentPending(false).secret("secret").build();
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(" USER@example.com ").password("password").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(mfaChallengeService.create(7L, MfaChallengeService.Purpose.LOGIN)).thenReturn("challenge");

        AuthenticationResponse response = service.authenticate(request);

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(
                "user@example.com", "password"));
        assertThat(response.isMfaRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("challenge");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getSecretImageUri()).isNull();
        assertThat(response.getQrCodeImageUri()).isNull();
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void wrongPasswordCannotCreateMfaChallengeEvenWithKnownTotp() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("user@example.com").password("wrong").build();
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(mfaChallengeService, never()).create(any(), any());
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void otpVerificationWithoutPasswordChallengeFailsBeforeUserLookup() {
        VerificationRequest request = VerificationRequest.builder()
                .challengeToken("missing").code("123456").build();
        when(mfaChallengeService.inspect("missing")).thenThrow(MfaVerificationException.expired());

        assertThatThrownBy(() -> service.verifyLoginCode(request))
                .isInstanceOf(MfaVerificationException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).findByIdForUpdate(any());
        verify(tfaService, never()).isOtpNotValid(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void validOtpConsumesChallengeBeforeIssuingAccessAndRefreshTokens() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .mfaEnrollmentPending(false).secret("secret").build();
        MfaChallengeService.Challenge challenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.LOGIN, java.time.Instant.now().plusSeconds(60), 0);
        VerificationRequest request = VerificationRequest.builder()
                .challengeToken("challenge").code("123456").build();
        when(mfaChallengeService.inspect("challenge")).thenReturn(challenge);
        when(mfaChallengeService.consume("challenge")).thenReturn(challenge);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(tfaService.isOtpNotValid("secret", "123456")).thenReturn(false);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        AuthenticationResponse response = service.verifyLoginCode(request);

        InOrder order = inOrder(mfaChallengeService, jwtService, tokenRepository);
        order.verify(mfaChallengeService).consume("challenge");
        order.verify(jwtService).generateToken(user);
        order.verify(jwtService).generateRefreshToken(user);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(tokenRepository, times(2)).save(any());
    }

    @Test
    void doctorEnrollmentEnablesMfaWithoutChangingPendingApprovalState() {
        Doctor doctor = new Doctor();
        doctor.setId(8L);
        doctor.setEmail("doctor@example.com");
        doctor.setPassword("encoded-password");
        doctor.setRole(Role.DOCTOR);
        doctor.setState(DoctorState.PENDING);
        doctor.setSecret("secret");
        doctor.setMfaEnrollmentPending(true);
        MfaChallengeService.Challenge challenge = new MfaChallengeService.Challenge(
                8L, MfaChallengeService.Purpose.ENROLLMENT, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("challenge")).thenReturn(challenge);
        when(mfaChallengeService.consume("challenge")).thenReturn(challenge);
        when(userRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(doctor));
        when(tfaService.isOtpNotValid("secret", "123456")).thenReturn(false);
        when(jwtService.generateToken(doctor)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(doctor)).thenReturn("refresh-token");

        service.verifyEnrollmentCode(VerificationRequest.builder()
                .challengeToken("challenge").code("123456").build());

        assertThat(doctor.isMfaEnabled()).isTrue();
        assertThat(doctor.isMfaEnrollmentPending()).isFalse();
        assertThat(doctor.getState()).isEqualTo(DoctorState.PENDING);
        verify(userRepository).save(doctor);
    }

    @Test
    void invalidOtpRecordsFailureAndIssuesNoTokens() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .mfaEnrollmentPending(false).secret("secret").build();
        MfaChallengeService.Challenge challenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.LOGIN, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("challenge")).thenReturn(challenge);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(tfaService.isOtpNotValid("secret", "000000")).thenReturn(true);

        assertThatThrownBy(() -> service.verifyLoginCode(VerificationRequest.builder()
                .challengeToken("challenge").code("000000").build()))
                .isInstanceOf(MfaVerificationException.class);

        verify(mfaChallengeService).recordFailure("challenge");
        verify(mfaChallengeService, never()).consume(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void pendingEnrollmentLoginReturnsResumeChallengeWithoutQrCode() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(false)
                .mfaEnrollmentPending(true).secret("unverified-secret").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(mfaChallengeService.create(7L, MfaChallengeService.Purpose.ENROLLMENT_RESUME))
                .thenReturn("resume-challenge");

        AuthenticationResponse response = service.authenticate(AuthenticationRequest.builder()
                .email("user@example.com").password("password").build());

        assertThat(response.isMfaEnrollmentRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("resume-challenge");
        assertThat(response.getQrCodeImageUri()).isNull();
        assertThat(response.getSecretImageUri()).isNull();
        verify(tfaService, never()).generateQrCodeImageUri(any());
    }

    @Test
    void inconsistentLegacyMfaStateIsResetAndCurrentLoginIsRejected() {
        UserEntity user = UserEntity.builder().id(7L).email("legacy@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .secret(null).build();
        when(userRepository.findByEmail("legacy@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate(AuthenticationRequest.builder()
                .email("legacy@example.com").password("password").build()))
                .isInstanceOf(MfaEnrollmentStateException.class);

        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(user.isMfaEnrollmentPending()).isFalse();
        assertThat(user.getSecret()).isNull();
        verify(userRepository).save(user);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void enrollmentChallengeCannotBeUsedAtNormalLoginVerificationEndpoint() {
        MfaChallengeService.Challenge challenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.ENROLLMENT, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("enrollment-challenge")).thenReturn(challenge);

        assertThatThrownBy(() -> service.verifyLoginCode(VerificationRequest.builder()
                .challengeToken("enrollment-challenge").code("123456").build()))
                .isInstanceOf(MfaVerificationException.class);

        verify(userRepository, never()).findByIdForUpdate(any());
        verify(tfaService, never()).isOtpNotValid(any(), any());
    }

    @Test
    void enabledUserCanRotateAuthenticatorWithOneTimeLoginChallenge() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .mfaEnrollmentPending(false).secret("old-secret")
                .authenticationVersion(3).build();
        Token activeToken = Token.builder().user(user).token("old-token")
                .expired(false).revoked(false).build();
        MfaChallengeService.Challenge loginChallenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.LOGIN, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("login-challenge")).thenReturn(loginChallenge);
        when(mfaChallengeService.consume("login-challenge")).thenReturn(loginChallenge);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(tokenRepository.findAllValidTokensByUser(7L)).thenReturn(java.util.List.of(activeToken));
        when(tfaService.generateNewSecret()).thenReturn("new-secret");
        when(tfaService.generateQrCodeImageUri("new-secret")).thenReturn("data:image/png;base64,new-qr");
        when(mfaChallengeService.create(7L, MfaChallengeService.Purpose.ENROLLMENT))
                .thenReturn("enrollment-challenge");

        AuthenticationResponse response = service.resumeEnrollment(
                new MfaEnrollmentRequest("login-challenge"));

        assertThat(user.getSecret()).isEqualTo("new-secret");
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(user.isMfaEnrollmentPending()).isTrue();
        assertThat(user.getAuthenticationVersion()).isEqualTo(4);
        assertThat(activeToken.isExpired()).isTrue();
        assertThat(activeToken.isRevoked()).isTrue();
        assertThat(response.getQrCodeImageUri()).isEqualTo("data:image/png;base64,new-qr");
        assertThat(response.getMfaChallengeToken()).isEqualTo("enrollment-challenge");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        verify(tokenRepository).saveAll(java.util.List.of(activeToken));
    }

    @Test
    void usedLoginChallengeCannotStartReEnrollmentAgain() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(true)
                .mfaEnrollmentPending(false).secret("old-secret").build();
        MfaChallengeService.Challenge loginChallenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.LOGIN, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("login-challenge"))
                .thenReturn(loginChallenge)
                .thenThrow(MfaVerificationException.expired());
        when(mfaChallengeService.consume("login-challenge")).thenReturn(loginChallenge);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(tfaService.generateNewSecret()).thenReturn("new-secret");
        when(tfaService.generateQrCodeImageUri("new-secret")).thenReturn("data:image/png;base64,new-qr");
        when(mfaChallengeService.create(7L, MfaChallengeService.Purpose.ENROLLMENT))
                .thenReturn("enrollment-challenge");

        service.resumeEnrollment(new MfaEnrollmentRequest("login-challenge"));

        assertThatThrownBy(() -> service.resumeEnrollment(new MfaEnrollmentRequest("login-challenge")))
                .isInstanceOf(MfaVerificationException.class);
        verify(tfaService, times(1)).generateNewSecret();
    }

    @Test
    void nonLoginChallengeCannotStartEnabledUserRecovery() {
        MfaChallengeService.Challenge enrollmentChallenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.ENROLLMENT, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("wrong-purpose")).thenReturn(enrollmentChallenge);

        assertThatThrownBy(() -> service.resumeEnrollment(new MfaEnrollmentRequest("wrong-purpose")))
                .isInstanceOf(MfaVerificationException.class);

        verify(userRepository, never()).findByIdForUpdate(any());
        verify(tfaService, never()).generateNewSecret();
    }

    @Test
    void oldAuthenticatorCodeCannotCompleteNewEnrollment() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com")
                .password("encoded-password").role(Role.PATIENT).mfaEnabled(false)
                .mfaEnrollmentPending(true).secret("new-secret").build();
        MfaChallengeService.Challenge enrollmentChallenge = new MfaChallengeService.Challenge(
                7L, MfaChallengeService.Purpose.ENROLLMENT, java.time.Instant.now().plusSeconds(60), 0);
        when(mfaChallengeService.inspect("enrollment-challenge")).thenReturn(enrollmentChallenge);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(tfaService.isOtpNotValid("new-secret", "111111")).thenReturn(true);

        assertThatThrownBy(() -> service.verifyEnrollmentCode(VerificationRequest.builder()
                .challengeToken("enrollment-challenge").code("111111").build()))
                .isInstanceOf(MfaVerificationException.class);

        verify(tfaService).isOtpNotValid("new-secret", "111111");
        assertThat(user.isMfaEnabled()).isFalse();
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void publicRegistrationAlwaysCreatesPatientEvenWhenClientRequestsAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Patient")
                .lastname("Example")
                .email("Patient@example.com")
                .password("validPassword")
                .role(Role.ADMIN)
                .mfaEnabled(false)
                .build();

        when(userRepository.existsByEmail("patient@example.com")).thenReturn(false);
        when(passwordEncoder.encode("validPassword")).thenReturn("encoded-password");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserEntity.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserEntity.class))).thenReturn("refresh-token");

        service.register(request);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        assertThat(patientCaptor.getValue().getRole()).isEqualTo(Role.PATIENT);
        verify(userRepository, never()).save(any(Admin.class));
    }

    @Test
    void publicRegistrationAlwaysCreatesPatientEvenWhenClientRequestsDoctor() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Patient")
                .lastname("Example")
                .email("patient@example.com")
                .password("validPassword")
                .role(Role.DOCTOR)
                .build();

        when(passwordEncoder.encode("validPassword")).thenReturn("encoded-password");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(request);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        assertThat(patientCaptor.getValue().getRole()).isEqualTo(Role.PATIENT);
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void bootstrapCreatesPrivilegedAdminWithNormalizedEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Admin")
                .lastname("User")
                .email(" Admin@Example.com ")
                .password("validPassword")
                .role(Role.PATIENT)
                .mfaEnabled(true)
                .build();

        when(passwordEncoder.encode("validPassword")).thenReturn("encoded-password");
        when(userRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = service.createBootstrapAdmin(request);

        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.isCanManageUsers()).isTrue();
        assertThat(result.isCanManageSystem()).isTrue();
        assertThat(result.isMfaEnabled()).isFalse();
        assertThat(result.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void bootstrapIsIdempotentAndDoesNotOverwriteExistingAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Admin")
                .lastname("User")
                .email("ADMIN@example.com")
                .password("newPassword")
                .build();
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true, true);

        Admin firstResult = service.createBootstrapAdmin(request);
        Admin secondResult = service.createBootstrapAdmin(request);

        assertThat(firstResult).isNull();
        assertThat(secondResult).isNull();
        verify(userRepository, times(2)).existsByEmail("admin@example.com");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void registersPendingDoctorWithServerControlledRoleAfterValidation() {
        Specialty specialty = Specialty.builder().id(1L).name("CARDIOLOGY").build();
        DoctorRegisterRequest request = DoctorRegisterRequest.builder()
                .firstname("Doctor2")
                .lastname("Doctor2")
                .licenseNumber("abc-123")
                .email("Unique.Doctor@example.com")
                .password("validPassword")
                .specialtyId(1L)
                .role(Role.ADMIN)
                .mfaEnabled(false)
                .build();
        MockMultipartFile certification = new MockMultipartFile(
                "image", "Med Board.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(userRepository.existsByEmail("unique.doctor@example.com")).thenReturn(false);
        when(doctorRepository.existsByLicenseNumber("abc-123")).thenReturn(false);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(cloudinaryService.uploadImage(certification)).thenReturn("https://files.example/certification.jpg");
        when(passwordEncoder.encode("validPassword")).thenReturn("encoded-password");
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Doctor.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(Doctor.class))).thenReturn("refresh-token");

        AuthenticationResponse response = service.registerDoctor(request, certification);

        ArgumentCaptor<Doctor> doctorCaptor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(doctorCaptor.capture());
        Doctor savedDoctor = doctorCaptor.getValue();
        assertThat(savedDoctor.getEmail()).isEqualTo("unique.doctor@example.com");
        assertThat(savedDoctor.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(savedDoctor.getState()).isEqualTo(DoctorState.PENDING);
        assertThat(savedDoctor.getSpecialty()).isSameAs(specialty);
        assertThat(savedDoctor.getCertificationUrl()).isEqualTo("https://files.example/certification.jpg");
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        InOrder order = inOrder(userRepository, doctorRepository, specialtyRepository, cloudinaryService);
        order.verify(userRepository).existsByEmail("unique.doctor@example.com");
        order.verify(doctorRepository).existsByLicenseNumber("abc-123");
        order.verify(specialtyRepository).findById(1L);
        order.verify(cloudinaryService).uploadImage(certification);
    }
}
