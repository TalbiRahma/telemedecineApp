package com.telemedecine.api.dao;

import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);
    List<Doctor> findByState(DoctorState state);
    long countByState(DoctorState state);
}
