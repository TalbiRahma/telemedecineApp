package com.telemedecine.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedecine.api.dao.*;
import com.telemedecine.api.mapper.SpecialtyMapper;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.token.Token;
import com.telemedecine.api.model.token.TokenType;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.MfaState;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.exception.CertificationUploadException;
import com.telemedecine.api.exception.CertificationUploadConfigurationException;
import com.telemedecine.api.exception.DuplicateEmailException;
import com.telemedecine.api.exception.DuplicateLicenseNumberException;
import com.telemedecine.api.exception.SpecialtyNotFoundException;
import com.telemedecine.api.security.JwtService;
import com.telemedecine.api.security.tfa.TwoFactorAuthenticationService;
import com.telemedecine.api.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TwoFactorAuthenticationService tfaService;
    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyMapper specialtyMapper;
    private final CloudinaryService cloudinaryService;
    private final PatientRepository patientRepository;
    private final MfaChallengeService mfaChallengeService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request){
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        Patient patient = new Patient();
        patient.setFirstname(request.getFirstname());
        patient.setLastname(request.getLastname());
        patient.setEmail(normalizedEmail);
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setRole(Role.PATIENT);
        patient.setMfaEnabled(false);
        patient.setMfaEnrollmentPending(request.isMfaEnabled());

        if (request.isMfaEnabled()) {
            patient.setSecret(tfaService.generateNewSecret());
        }

        Patient savedPatient = patientRepository.save(patient);
        if (request.isMfaEnabled()) {
            return createEnrollmentResponse(savedPatient);
        }
        return issueTokens(savedPatient);
    }

    /**
     * Internal bootstrap operation. This method is intentionally not exposed by
     * {@link AuthenticationController}; public registration remains patient-only.
     *
     * @return the newly created admin, or {@code null} when the email is already in use
     */
    @Transactional
    public Admin createBootstrapAdmin(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Bootstrap admin already exists; skipping creation.");
            return null;
        }

        Admin admin = new Admin();
        admin.setFirstname(request.getFirstname());
        admin.setLastname(request.getLastname());
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(Role.ADMIN);
        admin.setMfaEnabled(false);
        admin.setCanManageUsers(true);
        admin.setCanManageSystem(true);

        return userRepository.save(admin);
    }

    @Transactional
    public AuthenticationResponse registerDoctor(DoctorRegisterRequest request, MultipartFile image) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedLicenseNumber = request.getLicenseNumber().trim();
        log.info("Starting doctor registration for email {}", normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        if (doctorRepository.existsByLicenseNumber(normalizedLicenseNumber)) {
            throw new DuplicateLicenseNumberException();
        }

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(SpecialtyNotFoundException::new);

        String imageUrl;
        log.info("Uploading doctor certification for email {}", normalizedEmail);
        try {
            imageUrl = cloudinaryService.uploadImage(image);
        } catch (CertificationUploadException | CertificationUploadConfigurationException exception) {
            Throwable rootCause = exception.getCause() == null ? exception : exception.getCause();
            log.error("Doctor certification upload failed for email {}: {}: {}",
                    normalizedEmail, rootCause.getClass().getName(), rootCause.getMessage(), exception);
            throw exception;
        }
        log.info("Certification upload successful for email {}", normalizedEmail);

        Doctor doctor = new Doctor();
        doctor.setFirstname(request.getFirstname());
        doctor.setLastname(request.getLastname());
        doctor.setEmail(normalizedEmail);
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));
        doctor.setRole(Role.DOCTOR);
        doctor.setCertificationUrl(imageUrl);
        doctor.setLicenseNumber(normalizedLicenseNumber);
        doctor.setSpecialty(specialty);
        doctor.setState(DoctorState.PENDING);
        doctor.setMfaEnabled(false);
        doctor.setMfaEnrollmentPending(request.isMfaEnabled());

        // if MFA enabled --> Generate Secret
        if (request.isMfaEnabled()) {
            String secret = tfaService.generateNewSecret();
            doctor.setSecret(secret);
        }

        var savedDoctor = doctorRepository.save(doctor);

        if (request.isMfaEnabled()) {
            return createEnrollmentResponse(savedDoctor);
        }
        return issueTokens(savedDoctor);
    }



    public AuthenticationResponse authenticate(AuthenticationRequest request){
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow();
        MfaState mfaState = user.getMfaState();
        if (mfaState == MfaState.INCONSISTENT) {
            resetInconsistentMfaState(user);
            throw new MfaEnrollmentStateException(
                    "This account had an incomplete legacy MFA setup. MFA was safely reset; sign in again and enroll from account security.");
        }
        if (mfaState == MfaState.ENABLED) {
            return  AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .mfaRequired(true)
                    .mfaChallengeToken(mfaChallengeService.create(
                            user.getId(), MfaChallengeService.Purpose.LOGIN))
                    .build();
        }
        if (mfaState == MfaState.ENROLLMENT_PENDING) {
            return AuthenticationResponse.builder()
                    .mfaEnabled(false)
                    .mfaRequired(true)
                    .mfaEnrollmentRequired(true)
                    .mfaChallengeToken(mfaChallengeService.create(
                            user.getId(), MfaChallengeService.Purpose.ENROLLMENT_RESUME))
                    .build();
        }
        return issueTokens(user);
    }

    private void revokeAllUserTokens(UserEntity user){
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()){
            return;
        }
        validUserTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    private void saveUserToken(UserEntity user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();

        tokenRepository.save(token);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token is required");
            return;
        }
        try {
            refreshToken = authorizationHeader.substring(7);
            userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail != null) {
                var user = this.userRepository.findByEmail(userEmail).orElseThrow();
                boolean isStoredTokenValid = tokenRepository.findByToken(refreshToken)
                        .map(token -> !token.isExpired() && !token.isRevoked())
                        .orElse(false);
                if (isStoredTokenValid
                        && jwtService.isRefreshToken(refreshToken)
                        && jwtService.isTokenValid(refreshToken, user)) {
                    var accessToken = jwtService.generateToken(user);
                    saveUserToken(user, accessToken);
                    var authResponse = AuthenticationResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .mfaEnabled(user.isMfaEnabled())
                            .mfaRequired(false)
                            .build();
                    response.setContentType("application/json");
                    new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                    return;
                }
            }
        } catch (RuntimeException exception) {
            // Invalid and expired refresh tokens are authentication failures, not server errors.
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired refresh token");
    }

    @Transactional
    public AuthenticationResponse verifyLoginCode(VerificationRequest verificationRequest) {
        return verifyCode(verificationRequest, MfaChallengeService.Purpose.LOGIN);
    }

    @Transactional
    public AuthenticationResponse verifyEnrollmentCode(VerificationRequest verificationRequest) {
        return verifyCode(verificationRequest, MfaChallengeService.Purpose.ENROLLMENT);
    }

    @Transactional
    public AuthenticationResponse resumeEnrollment(MfaEnrollmentRequest request) {
        MfaChallengeService.Challenge challenge = mfaChallengeService.inspect(request.challengeToken());
        boolean pendingResume = challenge.purpose() == MfaChallengeService.Purpose.ENROLLMENT_RESUME;
        boolean enabledRecovery = challenge.purpose() == MfaChallengeService.Purpose.LOGIN;
        if (!pendingResume && !enabledRecovery) {
            throw MfaVerificationException.expired();
        }
        UserEntity user = userRepository.findByIdForUpdate(challenge.userId())
                .orElseThrow(MfaVerificationException::expired);
        if ((pendingResume && user.getMfaState() != MfaState.ENROLLMENT_PENDING)
                || (enabledRecovery && user.getMfaState() != MfaState.ENABLED)) {
            throw MfaVerificationException.expired();
        }
        MfaChallengeService.Challenge consumed = mfaChallengeService.consume(request.challengeToken());
        if (!consumed.userId().equals(user.getId())
                || consumed.purpose() != challenge.purpose()) {
            throw MfaVerificationException.expired();
        }

        if (enabledRecovery) {
            revokeAllUserTokens(user);
            int currentVersion = user.getAuthenticationVersion() == null
                    ? 0 : user.getAuthenticationVersion();
            user.setAuthenticationVersion(currentVersion + 1);
        }
        user.setMfaEnabled(false);
        user.setMfaEnrollmentPending(true);
        // Always rotate: neither pending nor verified secrets are shown again.
        user.setSecret(tfaService.generateNewSecret());
        userRepository.save(user);
        return createEnrollmentResponse(user);
    }

    @Transactional
    public AuthenticationResponse beginEnrollment(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(MfaVerificationException::expired);
        if (user.getMfaState() == MfaState.ENABLED) {
            throw new MfaEnrollmentStateException(
                    "MFA is already enabled. Verify the current authenticator before reconfiguration.");
        }
        user.setMfaEnabled(false);
        user.setMfaEnrollmentPending(true);
        user.setSecret(tfaService.generateNewSecret());
        userRepository.save(user);
        return createEnrollmentResponse(user);
    }

    private AuthenticationResponse verifyCode(
            VerificationRequest verificationRequest,
            MfaChallengeService.Purpose expectedPurpose
    ) {
        MfaChallengeService.Challenge challenge =
                mfaChallengeService.inspect(verificationRequest.getChallengeToken());
        if (challenge.purpose() != expectedPurpose) {
            throw MfaVerificationException.expired();
        }
        UserEntity user = userRepository.findByIdForUpdate(challenge.userId())
                .orElseThrow(MfaVerificationException::expired);

        if (challenge.purpose() == MfaChallengeService.Purpose.LOGIN
                && user.getMfaState() != MfaState.ENABLED) {
            throw MfaVerificationException.expired();
        }
        if (challenge.purpose() == MfaChallengeService.Purpose.ENROLLMENT
                && user.getMfaState() != MfaState.ENROLLMENT_PENDING) {
            throw MfaVerificationException.expired();
        }
        if (tfaService.isOtpNotValid(user.getSecret(), verificationRequest.getCode())) {
            mfaChallengeService.recordFailure(verificationRequest.getChallengeToken());
            throw MfaVerificationException.invalidCode();
        }

        MfaChallengeService.Challenge consumed =
                mfaChallengeService.consume(verificationRequest.getChallengeToken());
        if (!consumed.userId().equals(user.getId()) || consumed.purpose() != challenge.purpose()) {
            throw MfaVerificationException.expired();
        }

        if (challenge.purpose() == MfaChallengeService.Purpose.ENROLLMENT) {
            user.setMfaEnrollmentPending(false);
            user.setMfaEnabled(true);
            userRepository.save(user);
        }
        return issueTokens(user);
    }

    private void resetInconsistentMfaState(UserEntity user) {
        user.setMfaEnabled(false);
        user.setMfaEnrollmentPending(false);
        user.setSecret(null);
        revokeAllUserTokens(user);
        userRepository.save(user);
    }

    private AuthenticationResponse createEnrollmentResponse(UserEntity user) {
        String qrCodeImageUri = tfaService.generateQrCodeImageUri(user.getSecret());
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .mfaRequired(true)
                .mfaEnrollmentRequired(true)
                .mfaChallengeToken(mfaChallengeService.create(
                        user.getId(), MfaChallengeService.Purpose.ENROLLMENT))
                .qrCodeImageUri(qrCodeImageUri)
                .build();
    }

    private AuthenticationResponse issueTokens(UserEntity user) {
        revokeAllUserTokens(user);
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, accessToken);
        saveUserToken(user, refreshToken);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaEnabled(user.isMfaEnabled())
                .mfaRequired(false)
                .mfaEnrollmentRequired(false)
                .build();
    }
}
