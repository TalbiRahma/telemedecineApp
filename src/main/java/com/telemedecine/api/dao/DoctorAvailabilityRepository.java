package com.telemedecine.api.dao;

import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorId(Long doctorId);
    List<DoctorAvailability> findByDoctorIdAndType(Long doctorId, AvailabilityType type);
    List<DoctorAvailability> findByDoctorIdAndDate(Long doctorId, LocalDate date);
}
