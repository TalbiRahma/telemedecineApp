package com.telemedecine.api.service;

import com.telemedecine.api.auth.ChangePasswordRequest;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.model.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void changePasswored(ChangePasswordRequest request, Principal connectedUser) {

        var user =  (UserEntity)((UsernamePasswordAuthenticationToken)connectedUser).getPrincipal();

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        // check if the two new passwords are the same
        if( ! request.getNewPassword().equals(request.getConfirmNewPassword()) ) {
            throw new IllegalStateException("Passwords don't match");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // save the new password
        userRepository.save(user);
    }
}
