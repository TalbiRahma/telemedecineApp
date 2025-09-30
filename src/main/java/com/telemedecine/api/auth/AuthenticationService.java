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
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.security.JwtService;
import com.telemedecine.api.security.tfa.TwoFactorAuthenticationService;
import com.telemedecine.api.service.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
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

    public AuthenticationResponse register(RegisterRequest request){
        UserEntity user;

        switch (request.getRole()) {
            case PATIENT:
                user = new Patient();
                break;
            case ADMIN:
                user = new Admin();
                break;
            default:
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setMfaEnabled(request.isMfaEnabled());


        // if MFA enabled --> Generate Secret
        if (request.isMfaEnabled()) {
            user.setSecret(tfaService.generateNewSecret());
        }

        var savedUser = userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .secretImageUri(tfaService.generateQrCodeImageUri(user.getSecret()))
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }

    public AuthenticationResponse registerDoctor(DoctorRegisterRequest request, MultipartFile image) throws IOException{

        // Upload license file
        String imageUrl = cloudinaryService.uploadImage(image);

        // Fetch specialty by ID from the repository
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new RuntimeException("Specialty not found with id: " + request.getSpecialtyId()));

        Doctor doctor = new Doctor();
        doctor.setFirstname(request.getFirstname());
        doctor.setLastname(request.getLastname());
        doctor.setEmail(request.getEmail());
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));
        doctor.setRole(request.getRole());
        doctor.setCertificationUrl(imageUrl);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setSpecialty(specialty);
        doctor.setState(DoctorState.PENDING);
        doctor.setMfaEnabled(request.isMfaEnabled());

        // if MFA enabled --> Generate Secret
        if (request.isMfaEnabled()) {
            String secret = tfaService.generateNewSecret();
            doctor.setSecret(secret);
        }

        var savedDoctor = doctorRepository.save(doctor);

        String secretImageUri = null;
        if (savedDoctor.isMfaEnabled()) {
            secretImageUri = tfaService.generateQrCodeImageUri(savedDoctor.getSecret());
        }

        var jwtToken = jwtService.generateToken(doctor);
        var refreshToken = jwtService.generateRefreshToken(savedDoctor);

        saveUserToken(savedDoctor, jwtToken);

        // Create response without builder
        AuthenticationResponse response = new AuthenticationResponse();
        response.setSecretImageUri(secretImageUri);
        response.setAccessToken(jwtToken);
        response.setRefreshToken(refreshToken);
        response.setMfaEnabled(doctor.isMfaEnabled());

        return response;
    }



    public AuthenticationResponse authenticate(AuthenticationRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        if (user.isMfaEnabled()) {
            String qrCodeImageUri = tfaService.generateQrCodeImageUri(user.getSecret());
            return  AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .secretImageUri(qrCodeImageUri)
                    .accessToken("")
                    .refreshToken("")
                    .build();
        }
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .mfaEnabled(false)
                .build();
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
            return;
        }
        refreshToken = authorizationHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .mfaEnabled(false)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    public AuthenticationResponse verifyCode(
            VerificationRequest verificationRequest
    ) {
        UserEntity user = userRepository.findByEmail(verificationRequest.getEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("User with email %s not found", verificationRequest.getEmail())
                ));

        if ((tfaService.isOtpNotValid(user.getSecret(), verificationRequest.getCode()))) {
            throw new BadCredentialsException("Code is not correct");
        }
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user); // Add this
        revokeAllUserTokens(user); // Add this
        saveUserToken(user, jwtToken); // Add this

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)// Add this
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }
}
