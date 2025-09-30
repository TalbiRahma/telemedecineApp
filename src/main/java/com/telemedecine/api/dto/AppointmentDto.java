package com.telemedecine.api.dto;

import com.telemedecine.api.model.appointement.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDto {
    private Long id;
    private LocalDateTime bookedAt;
    private AppointmentStatus status;

    private Long slotId;
    private Long patientId;
}