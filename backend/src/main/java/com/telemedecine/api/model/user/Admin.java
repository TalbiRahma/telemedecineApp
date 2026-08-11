package com.telemedecine.api.model.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admin")
@DiscriminatorValue("ADMIN")
@PrimaryKeyJoinColumn(name = "user_id")
public class Admin  extends UserEntity{

    @Column(name = "admin_level")
    @Enumerated(EnumType.STRING)
    private AdminLevel adminLevel;

    @Column(name = "department")
    private String department;

    @Column(name = "can_manage_users", nullable = false)
    private boolean canManageUsers = true;

    @Column(name = "can_manage_system", nullable = false)
    private boolean canManageSystem = true;

    public enum AdminLevel {
        SUPER_ADMIN,
        MANAGER,
        SUPPORT
    }

}
