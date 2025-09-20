package com.telemedecine.api.controller;

import com.telemedecine.api.dto.DoctorDto;
import com.telemedecine.api.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;


    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('doctor:update','doctor:manage')")
    public ResponseEntity<DoctorDto> update(@PathVariable Long id, @Valid @RequestBody DoctorDto dto) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doctor:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyAuthority('doctor:read','doctor:manage')")
    public ResponseEntity<DoctorDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('doctor:read','doctor:manage')")
    public ResponseEntity<List<DoctorDto>> getAll() {
        return ResponseEntity.ok(doctorService.getAll());
    }

}
