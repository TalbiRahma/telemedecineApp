package com.telemedecine.api.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.token.Token;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public class UserEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Firstname is required")
    @Size(min = 2, max = 50, message = "Firstname must be between 2 and 50 characters")
    private String firstname;

    @NotBlank(message = "Lastname is required")
    @Size(min = 2, max = 50, message = "Lastname must be between 2 and 50 characters")
    private String lastname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
    @JsonIgnore
    private String password;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    /** True only after the first TOTP from {@link #secret} has been verified. */
    private boolean mfaEnabled;
    /** True while {@link #secret} is an unverified enrollment secret. */
    @Column(nullable = true)
    private Boolean mfaEnrollmentPending;
    @JsonIgnore
    private String secret;

    private Instant credentialsUpdatedAt;

    private Integer authenticationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false)
    private Role role;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Token>  tokens;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    public boolean isMfaEnrollmentPending() {
        return Boolean.TRUE.equals(mfaEnrollmentPending);
    }

    public void setMfaEnrollmentPending(boolean mfaEnrollmentPending) {
        this.mfaEnrollmentPending = mfaEnrollmentPending;
    }

    @JsonIgnore
    public MfaState getMfaState() {
        boolean hasSecret = secret != null && !secret.isBlank();
        if (mfaEnabled && Boolean.FALSE.equals(mfaEnrollmentPending) && hasSecret) {
            return MfaState.ENABLED;
        }
        if (!mfaEnabled && Boolean.TRUE.equals(mfaEnrollmentPending) && hasSecret) {
            return MfaState.ENROLLMENT_PENDING;
        }
        if (!mfaEnabled && !Boolean.TRUE.equals(mfaEnrollmentPending) && !hasSecret) {
            return MfaState.DISABLED;
        }
        return MfaState.INCONSISTENT;
    }


    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
