package com.telemedecine.api.controller;

import com.telemedecine.api.dto.PatientDto;
import com.telemedecine.api.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientDto> getMe(Authentication authentication) {
        return ResponseEntity.ok(patientService.getCurrentPatient(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT') and hasAuthority('patient:update')")
    public ResponseEntity<PatientDto> updateMe(
            Authentication authentication,
            @Valid @RequestBody PatientDto dto
    ) {
        return ResponseEntity.ok(patientService.updateCurrentPatient(authentication.getName(), dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<PatientDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PatientDto>> getAll() {
        return ResponseEntity.ok(patientService.getAll());
    }
}
