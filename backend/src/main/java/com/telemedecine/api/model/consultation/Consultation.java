package com.telemedecine.api.model.consultation;

import com.telemedecine.api.model.ExaminationResult;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.prescription.Prescription;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.doctor.Doctor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "consultation")
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    private ConsultationType type;

    private String notes;

    @Enumerated(EnumType.STRING)
    private ConsultationStatus status = ConsultationStatus.SCHEDULED;

    // Zoom info (optionnel si type = ZOOM)
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomStartUrl;


    @ManyToOne
    private Patient patient;

    @ManyToOne
    private Doctor doctor;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL)
    private List<Prescription> prescriptions;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL)
    private List<ExaminationResult> examinationResults;
}
