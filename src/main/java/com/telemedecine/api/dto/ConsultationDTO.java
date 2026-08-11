package com.telemedecine.api.dto;

import com.telemedecine.api.model.consultation.ConsultationStatus;
import com.telemedecine.api.model.consultation.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsultationDTO {

    private Long id;
    private LocalDateTime date;
    private ConsultationType type;
    private String notes;
    private ConsultationStatus status;

    // Zoom Info (si type = ZOOM)
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomStartUrl;

    // Relations simplifiées (IDs uniquement)
    private Long patientId;
    private Long doctorId;
    private Long appointmentId;

    // Pour affichage rapide dans frontend
    private String patientName;
    private String doctorName;
}
