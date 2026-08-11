package com.telemedecine.api.controller;

import com.telemedecine.api.dto.DoctorDto;
import com.telemedecine.api.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;


    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DOCTOR') and #id == authentication.principal.id)")
    public ResponseEntity<DoctorDto> update(
            @PathVariable Long id, @Valid @RequestBody DoctorDto dto, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(doctorService.updateDoctor(id, dto, isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<DoctorDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<DoctorDto>> getAll() {
        return ResponseEntity.ok(doctorService.getAll());
    }

    @GetMapping("/bookable")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DoctorDto>> getBookableDoctors() {
        return ResponseEntity.ok(doctorService.getBookableDoctors());
    }

    @GetMapping("/bookable/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<DoctorDto> getBookableDoctor(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getBookableDoctor(id));
    }

}
