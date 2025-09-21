package com.telemedecine.api.controller;

import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService service;
    private final DoctorAvailabilityRepository repository;

    @PostMapping("/add")
    public DoctorAvailabilityDTO create(@RequestBody DoctorAvailabilityDTO dto) {
        return service.createAvailability(dto);
    }

    @GetMapping("/{doctorId}/all")
    public List<DoctorAvailabilityDTO> getAll(@PathVariable Long doctorId) {
        return service.getDoctorAvailabilities(doctorId);
    }

    @GetMapping("/{doctorId}/date")
    public List<DoctorAvailabilityDTO> getByDate(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDoctorAvailabilitiesByDate(doctorId, date);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found");
        }
        service.deleteAvailability(id);
    }

}
