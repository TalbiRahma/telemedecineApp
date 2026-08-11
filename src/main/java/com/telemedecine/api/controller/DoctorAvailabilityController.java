package com.telemedecine.api.controller;

import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService service;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public DoctorAvailabilityDTO create(
            @RequestBody DoctorAvailabilityDTO dto,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        return service.createAvailability(dto, authenticatedUser);
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorAvailabilityDTO createForCurrentDoctor(
            @RequestBody DoctorAvailabilityDTO dto,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        return service.createAvailability(dto, authenticatedUser);
    }

    @GetMapping("/{doctorId:\\d+}/all")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public List<DoctorAvailabilityDTO> getAll(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        requireSelfOrAdmin(doctorId, authenticatedUser);
        return service.getDoctorAvailabilities(doctorId);
    }

    @GetMapping("/me/all")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<DoctorAvailabilityDTO> getAllForCurrentDoctor(
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        return service.getDoctorAvailabilities(authenticatedUser.getId());
    }

    @GetMapping("/{doctorId:\\d+}/date")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public List<DoctorAvailabilityDTO> getByDate(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        requireSelfOrAdmin(doctorId, authenticatedUser);
        return service.getDoctorAvailabilitiesByDate(doctorId, date);
    }

    @GetMapping("/{doctorId:\\d+}/occurrences")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public List<DoctorAvailabilityDTO> getOccurrences(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeSlots,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        requireSelfOrAdmin(doctorId, authenticatedUser);
        return service.getDoctorAvailabilityOccurrences(doctorId, from, to, includeSlots);
    }

    @GetMapping("/me/occurrences")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<DoctorAvailabilityDTO> getOccurrencesForCurrentDoctor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeSlots,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        return service.getDoctorAvailabilityOccurrences(authenticatedUser.getId(), from, to, includeSlots);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        service.deleteAvailability(id, authenticatedUser);
    }

    @DeleteMapping("/me/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForCurrentDoctor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity authenticatedUser) {
        service.deleteAvailability(id, authenticatedUser);
    }

    private void requireSelfOrAdmin(Long doctorId, UserEntity authenticatedUser) {
        if (authenticatedUser.getRole() != Role.ADMIN && !authenticatedUser.getId().equals(doctorId)) {
            throw new AccessDeniedException("Cannot manage another doctor's availability");
        }
    }
}
