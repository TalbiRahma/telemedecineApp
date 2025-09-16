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
public class RegisterRequest {

    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private Role role;
    private SpecialtyDto specialty;

    private boolean mfaEnabled;
}
