package com.telemedecine.api.dao;

import com.telemedecine.api.model.user.doctor.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);
}
