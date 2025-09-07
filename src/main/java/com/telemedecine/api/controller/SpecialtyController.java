package com.telemedecine.api.controller;

import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping("/all")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SpecialtyDto>> getAll() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyDto> getSpecialtyById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(specialtyService.getSpecialtyById(id));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpecialtyDto> create(@RequestBody SpecialtyDto dto) {
        return ResponseEntity.ok(specialtyService.createSpecialty(dto));
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpecialtyDto> update(
            @PathVariable Long id,
            @RequestBody SpecialtyDto dto
    ){
        return ResponseEntity.ok(specialtyService.updateSpecialty(dto, id));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSpecialty(
            @PathVariable Long id
    ) {
        try {
            specialtyService.deleteSpecialty(id);
            return ResponseEntity.ok("Deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression: " + e.getMessage());
        }
    }
}
