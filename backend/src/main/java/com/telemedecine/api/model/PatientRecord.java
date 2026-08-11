package com.telemedecine.api.model;

import com.telemedecine.api.model.user.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "patient_record")
public class PatientRecord {

    @Id
    @GeneratedValue
    private Long id;
    private String allergies;
    private String chronicDiseases;
    private String diagnosisHistory;
    private LocalDate lastUpdated;

    @OneToOne
    private Patient patient;
}
