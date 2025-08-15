package com.telemedecine.api.presentation.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctor")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    @GetMapping
    @PreAuthorize("hasAuthority('doctor:read')")
    public String get() {
        return "GET:: doctor controller";
    }


    @PostMapping
    @PreAuthorize("hasAuthority('doctor:create')")
    public String post() {
        return "POST:: doctor controller";
    }


    @PutMapping
    @PreAuthorize("hasAuthority('doctor:update')")
    public String put() {
        return "PUT:: doctor controller";
    }


    @DeleteMapping
    @PreAuthorize("hasAuthority('doctor:delete')")
    public String delete() {
        return "DELETE:: doctor controller";
    }
}
