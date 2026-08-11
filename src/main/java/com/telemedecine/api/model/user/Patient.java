package com.telemedecine.api.model.user;

import com.telemedecine.api.model.PatientRecord;
import com.telemedecine.api.model.consultation.Consultation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "patient")
@DiscriminatorValue("PATIENT")
@PrimaryKeyJoinColumn(name = "user_id")
public class Patient extends UserEntity {

    private LocalDate dateOfBirth;
    private String gender;

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL)
    private PatientRecord record;


    @OneToMany(mappedBy = "patient")
    private List<Consultation> consultations;
}