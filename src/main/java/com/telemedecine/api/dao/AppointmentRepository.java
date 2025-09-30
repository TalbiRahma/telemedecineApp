package com.telemedecine.api.dao;

import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.user.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatient(Patient patient);

    List<Appointment> findByPatientId(Long patientId);

    Appointment findBySlotId(Long slotId);
}
