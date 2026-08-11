package com.telemedecine.api.dao;

import com.telemedecine.api.model.consultation.Consultation;
import com.telemedecine.api.model.consultation.ConsultationStatus;
import com.telemedecine.api.model.consultation.ConsultationType;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.doctor.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 🔹 Trouver consultation par meetingId (utilisé par ZoomWebhookController)
    Optional<Consultation> findByZoomMeetingId(String zoomMeetingId);

    // 🔹 Lister les consultations d’un médecin
    List<Consultation> findByDoctor(Doctor doctor);

    // 🔹 Lister les consultations d’un patient
    List<Consultation> findByPatient(Patient patient);

    // 🔹 Trouver les consultations planifiées après une certaine date
    List<Consultation> findByDateAfter(LocalDateTime date);

    // 🔹 Trouver les consultations par type
    List<Consultation> findByType(ConsultationType type);

    // 🔹 Trouver les consultations par statut
    List<Consultation> findByStatus(ConsultationStatus status);
}
