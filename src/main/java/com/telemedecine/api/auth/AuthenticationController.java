package com.telemedecine.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        var response = service.register(request);
        if (request.isMfaEnabled()){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .build()
        );
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<AuthenticationResponse> registerDoctor(
            @RequestPart("request") String requestJson,
            @RequestParam("image") MultipartFile image) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        DoctorRegisterRequest request = objectMapper.readValue(requestJson, DoctorRegisterRequest.class);

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

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(
            @RequestBody VerificationRequest verificationRequest
    ) {
        return ResponseEntity.ok(service.verifyCode(verificationRequest));
    }
}
