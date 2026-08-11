package com.telemedecine.api.dto;

import com.telemedecine.api.model.slot.SlotStatus;
import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class SlotDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private SlotStatus status;
    private Long availabilityId;
}
