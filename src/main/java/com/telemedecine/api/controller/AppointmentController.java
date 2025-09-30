package com.telemedecine.api.controller;

import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<AppointmentDto> bookAppointment(@RequestBody AppointmentDto dto) {
        return ResponseEntity.ok(appointmentService.bookAppointment(dto));
    }


    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointment(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<AppointmentDto> completeAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    @PutMapping("/{id}/noshow")
    public ResponseEntity<AppointmentDto> markNoShow(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.markNoShow(id));
    }
}
