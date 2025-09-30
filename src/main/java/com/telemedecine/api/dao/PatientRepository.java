package com.telemedecine.api.dao;

import com.telemedecine.api.model.user.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
