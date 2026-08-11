package com.telemedecine.api.dto;

import com.telemedecine.api.model.user.doctor.AvailabilityType;
import lombok.Data;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
public class DoctorAvailabilityDTO {
    private Long id;
    private Long doctorId;
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AvailabilityType type;
    private Integer slotDuration;
    private List<SlotDto> slots;
}
