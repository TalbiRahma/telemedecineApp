package com.telemedecine.api.model.prescription;

import com.telemedecine.api.model.consultation.Consultation;
import com.telemedecine.api.model.user.doctor.Doctor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "prescription")
public class Prescription {
    @Id
    @GeneratedValue
    private Long id;
    private String medication;
    private String dosage;
    private String duration;
    private LocalDate datePrescribed;

    @ManyToOne
    private Doctor doctor;

    @ManyToOne
    private Consultation consultation;
}
