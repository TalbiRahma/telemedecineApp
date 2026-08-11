package com.telemedecine.api.model.user.doctor;

import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.user.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "doctor")
@DiscriminatorValue("DOCTOR")
@PrimaryKeyJoinColumn(name = "user_id")
public class Doctor extends UserEntity {


    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9-]{2,49}$",
            message = "License number contains invalid characters"
    )
    @Column(nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Size(min = 5, max = 255, message = "L'adresse doit contenir entre 5 et 255 caractères.")
    private String adresse;


    @Column(name = "certification_image")
    private String certificationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PENDING'")
    private DoctorState  state;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoctorAvailability> availabilities = new ArrayList<>();

}
