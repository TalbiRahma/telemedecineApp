package com.telemedecine.api.dto;

import com.telemedecine.api.model.user.doctor.AvailabilityType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DoctorAvailabilityDTO {
    private Long id;
    private Long doctorId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private AvailabilityType type;
}
