package com.telemedecine.api.dto;

import com.telemedecine.api.model.slot.SlotStatus;
import lombok.Data;

import java.time.LocalTime;

@Data
public class SlotDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;
    private Long availabilityId;
}
