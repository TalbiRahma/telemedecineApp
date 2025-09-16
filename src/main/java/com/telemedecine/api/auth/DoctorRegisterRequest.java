package com.telemedecine.api.auth;

import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.model.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorRegisterRequest {

    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private Role role;
    private String licenseNumber;
    private long specialtyId;
    private String certificationUrl;

    private boolean mfaEnabled;

}
