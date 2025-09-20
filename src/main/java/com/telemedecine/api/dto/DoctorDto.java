package com.telemedecine.api.dto;

import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.user.doctor.DoctorState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorDto {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String licenseNumber;
    private String adresse;
    private String certificationUrl;
    private DoctorState state;
    private SpecialtyDto specialty;
}
