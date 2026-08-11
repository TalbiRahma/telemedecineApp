package com.telemedecine.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.telemedecine.api.exception.InvalidRegistrationPayloadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final PasswordResetService passwordResetService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        var response = service.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<AuthenticationResponse> registerDoctor(
            @RequestPart("request") String requestJson,
            @RequestParam("image") MultipartFile image) {

        DoctorRegisterRequest request;
        try {
            request = objectMapper.readValue(requestJson, DoctorRegisterRequest.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidRegistrationPayloadException("Doctor registration details are invalid.", exception);
        }

        Set<ConstraintViolation<DoctorRegisterRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        AuthenticationResponse response = service.registerDoctor(request, image);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ){
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        service.refreshToken(request, response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyLoginCode(
            @Valid @RequestBody VerificationRequest verificationRequest
    ) {
        return ResponseEntity.ok(service.verifyLoginCode(verificationRequest));
    }

    @PostMapping("/mfa/enroll/verify")
    public ResponseEntity<AuthenticationResponse> verifyEnrollmentCode(
            @Valid @RequestBody VerificationRequest verificationRequest) {
        return ResponseEntity.ok(service.verifyEnrollmentCode(verificationRequest));
    }

    @PostMapping("/mfa/enroll/resume")
    public ResponseEntity<AuthenticationResponse> resumeEnrollment(
            @Valid @RequestBody MfaEnrollmentRequest request) {
        return ResponseEntity.ok(service.resumeEnrollment(request));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<AuthenticationResponse> beginEnrollment(Authentication authentication) {
        return ResponseEntity.ok(service.beginEnrollment(authentication.getName()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists for this email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully."));
    }
}
