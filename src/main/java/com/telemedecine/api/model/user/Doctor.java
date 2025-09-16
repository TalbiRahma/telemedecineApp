package com.telemedecine.api.model.user;

import com.telemedecine.api.model.Specialty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "doctor")
@DiscriminatorValue("DOCTOR")
public class Doctor extends UserEntity {


    @Pattern(
            regexp = "^[A-Za-z]{3}-?\\d{5,7}$",  // format: 3 lettres + 5-7 chiffres, tiret optionnel
            message = "License number must be 3 letters followed by 5 to 7 digits"
    )
    private String licenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Size(min = 5, max = 255, message = "L'adresse doit contenir entre 5 et 255 caractères.")
    private String adresse;


    @Column(name = "certification_image")
    private String certificationUrl;

}
