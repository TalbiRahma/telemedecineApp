package com.telemedecine.api.dao;

import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorId(Long doctorId);
    List<DoctorAvailability> findByDoctorIdAndType(Long doctorId, AvailabilityType type);
    List<DoctorAvailability> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    @Query("select distinct availability from DoctorAvailability availability "
            + "left join fetch availability.slots where availability.doctor.id = :doctorId")
    List<DoctorAvailability> findByDoctorIdWithSlots(@Param("doctorId") Long doctorId);
}
