package com.telemedecine.api.controller;

import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping
    public ResponseEntity<SlotDto> createSlot(@RequestBody SlotDto slotDto) {
        return ResponseEntity.ok(slotService.createSlot(slotDto));
    }

    @GetMapping("/availability/{availabilityId}")
    public ResponseEntity<List<SlotDto>> getSlotsByAvailability(@PathVariable Long availabilityId) {
        return ResponseEntity.ok(slotService.getSlotsByAvailability(availabilityId));
    }

    @PatchMapping("/{slotId}/status")
    public ResponseEntity<SlotDto> updateSlotStatus(
            @PathVariable Long slotId,
            @RequestBody SlotDto dto
    ) {
        return ResponseEntity.ok(slotService.updateSlotStatus(slotId, dto.getStatus()));
    }
}
