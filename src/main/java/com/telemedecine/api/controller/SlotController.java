package com.telemedecine.api.controller;

import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<SlotDto> createSlot(@RequestBody SlotDto slotDto) {
        return ResponseEntity.ok(slotService.createSlot(slotDto));
    }

    @GetMapping("/availability/{availabilityId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<SlotDto>> getSlotsByAvailability(@PathVariable Long availabilityId) {
        return ResponseEntity.ok(slotService.getSlotsByAvailability(availabilityId));
    }

    @PatchMapping("/{slotId}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<SlotDto> updateSlotStatus(
            @PathVariable Long slotId,
            @RequestBody SlotDto dto
    ) {
        return ResponseEntity.ok(slotService.updateSlotStatus(slotId, dto.getStatus()));
    }

    @PostMapping("/generate/{availabilityId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<SlotDto>> generateSlots(
            @PathVariable Long availabilityId,
            @RequestParam int durationMinutes
    ) {
        return ResponseEntity.ok(slotService.generateSlots(availabilityId, durationMinutes));
    }

    @GetMapping("/doctor/{doctorId}/free")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<SlotDto>> getFreeSlotsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to
    ) {
        return ResponseEntity.ok(slotService.getFreeSlotsByDoctor(doctorId, from, to));
    }
}
