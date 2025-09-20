package com.telemedecine.api.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "patient")
@DiscriminatorValue("PATIENT")
@PrimaryKeyJoinColumn(name = "user_id")
public class Patient extends UserEntity{

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String emergencyContact;

    private String bloodType;

    @Column(length = 500)
    private String medicalHistory;
}
