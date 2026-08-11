package com.telemedecine.api.dao;

import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import com.telemedecine.api.model.user.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByPatient(Patient patient);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findBySlotAvailabilityDoctorId(Long doctorId);

    boolean existsBySlotIdAndScheduledStartAndStatusIn(
            Long slotId, LocalDateTime scheduledStart, Collection<AppointmentStatus> statuses);

    List<Appointment> findBySlotAvailabilityDoctorIdAndScheduledStartBetweenAndStatusIn(
            Long doctorId, LocalDateTime from, LocalDateTime to, Collection<AppointmentStatus> statuses);
}
