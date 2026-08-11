package com.telemedecine.api.controller;

import com.telemedecine.api.exception.ApiErrorResponse;
import com.telemedecine.api.exception.CertificationUploadConfigurationException;
import com.telemedecine.api.exception.CertificationUploadException;
import com.telemedecine.api.exception.DuplicateEmailException;
import com.telemedecine.api.exception.DuplicateLicenseNumberException;
import com.telemedecine.api.exception.InvalidRegistrationPayloadException;
import com.telemedecine.api.exception.SpecialtyNotFoundException;
import com.telemedecine.api.exception.SlotUnavailableException;
import com.telemedecine.api.exception.InvalidResetTokenException;
import com.telemedecine.api.exception.EmailDeliveryException;
import com.telemedecine.api.exception.AiServiceUnavailableException;
import com.telemedecine.api.auth.MfaVerificationException;
import com.telemedecine.api.auth.MfaEnrollmentStateException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MfaEnrollmentStateException.class)
    public ResponseEntity<ApiErrorResponse> mfaEnrollmentState(MfaEnrollmentStateException exception) {
        return error(HttpStatus.CONFLICT, "MFA_ENROLLMENT_STATE_INVALID", exception.getMessage());
    }
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> aiServiceUnavailable(AiServiceUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", exception.getMessage());
    }
    @ExceptionHandler(MfaVerificationException.class)
    public ResponseEntity<ApiErrorResponse> mfaVerification(MfaVerificationException exception) {
        return switch (exception.getReason()) {
            case INVALID_CODE -> error(HttpStatus.BAD_REQUEST, "INVALID_MFA_CODE", exception.getMessage());
            case EXPIRED_CHALLENGE -> error(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED", exception.getMessage());
            case TOO_MANY_ATTEMPTS -> error(HttpStatus.TOO_MANY_REQUESTS, "MFA_TOO_MANY_ATTEMPTS", exception.getMessage());
        };
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ApiErrorResponse> invalidResetToken() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                "Password reset link is invalid or expired.");
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiErrorResponse> emailDeliveryFailed() {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_DELIVERY_FAILED",
                "We couldn't send the password reset email. Please try again later.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> accessDenied(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(CertificationUploadException.class)
    public ResponseEntity<ApiErrorResponse> certificationUploadFailed() {
        return error(HttpStatus.BAD_GATEWAY, "CERTIFICATION_UPLOAD_FAILED",
                "Unable to upload the certification document. Please try again.");
    }

    @ExceptionHandler(CertificationUploadConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> certificationUploadNotConfigured() {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "CERTIFICATION_UPLOAD_UNAVAILABLE",
                "Doctor certification upload service is not configured.");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> duplicateEmail() {
        return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL",
                "An account with this email already exists.");
    }

    @ExceptionHandler(DuplicateLicenseNumberException.class)
    public ResponseEntity<ApiErrorResponse> duplicateLicenseNumber() {
        return error(HttpStatus.CONFLICT, "DUPLICATE_LICENSE_NUMBER",
                "A doctor with this license number already exists.");
    }

    @ExceptionHandler(SpecialtyNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> specialtyNotFound() {
        return error(HttpStatus.NOT_FOUND, "SPECIALTY_NOT_FOUND",
                "Selected specialty does not exist.");
    }

    @ExceptionHandler(InvalidRegistrationPayloadException.class)
    public ResponseEntity<ApiErrorResponse> invalidRegistrationPayload() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_PAYLOAD",
                "Doctor registration details are invalid.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> constraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .sorted()
                .findFirst()
                .orElse("Registration details are invalid.");
        return error(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_PAYLOAD", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> methodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .sorted()
                .findFirst()
                .orElse("Registration details are invalid.");
        boolean aiRequest = exception.getParameter().getContainingClass().equals(AiPrediagnosticController.class);
        return error(HttpStatus.BAD_REQUEST,
                aiRequest ? "INVALID_AI_REQUEST" : "INVALID_REGISTRATION_PAYLOAD", message);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> missingMultipartPart(MissingServletRequestPartException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_PAYLOAD",
                "Required registration part is missing: " + exception.getRequestPartName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> uploadTooLarge() {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "CERTIFICATION_FILE_TOO_LARGE",
                "The certification document exceeds the allowed file size.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict() {
        return error(HttpStatus.CONFLICT, "REGISTRATION_CONFLICT",
                "An account with these details already exists.");
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> slotUnavailable(SlotUnavailableException exception) {
        return error(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message));
    }
}
