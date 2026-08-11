package com.telemedecine.api.controller;

import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT') and hasAuthority('appointment:create')")
    public ResponseEntity<AppointmentDto> bookAppointment(
            @RequestBody AppointmentDto dto, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.bookAppointment(dto, authentication.getName()));
    }

    @GetMapping("/doctors/{doctorId}/slots")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<SlotDto>> getBookableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentService.getBookableSlots(doctorId, from, to));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentDto> getAppointment(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getAppointment(id, authentication.getName()));
    }

    @GetMapping("/patient/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentDto>> getMyAppointments(Authentication authentication) {
        Long patientId = ((com.telemedecine.api.model.user.UserEntity) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId, authentication.getName()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByPatient(
            @PathVariable Long patientId, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId, authentication.getName()));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByDoctor(
            @PathVariable Long doctorId, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId, authentication.getName()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id, Authentication authentication) {
        appointmentService.cancelAppointment(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentDto> completeAppointment(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id, authentication.getName()));
    }

    @PutMapping("/{id}/noshow")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<AppointmentDto> markNoShow(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(appointmentService.markNoShow(id, authentication.getName()));
    }
}
