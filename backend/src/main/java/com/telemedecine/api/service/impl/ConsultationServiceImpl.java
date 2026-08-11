package com.telemedecine.api.service.impl;

import com.telemedecine.api.config.ZoomClient;
import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.ConsultationRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dto.ConsultationDTO;
import com.telemedecine.api.mapper.ConsultationMapper;
import com.telemedecine.api.model.ZoomMeetingInfo;
import com.telemedecine.api.model.consultation.Consultation;
import com.telemedecine.api.model.consultation.ConsultationStatus;
import com.telemedecine.api.model.consultation.ConsultationType;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.service.ConsultationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository repo;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ZoomClient zoomClient;
    private final ConsultationMapper mapper;


    @Transactional
    @Override
    public ConsultationDTO createConsultation(ConsultationDTO dto) {
        Consultation consultation = mapper.toEntity(dto);

        // Charger les entités liées (Doctor, Patient, Appointment)
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        consultation.setDoctor(doctor);
        consultation.setPatient(patient);

        if (dto.getAppointmentId() != null) {
            appointmentRepository.findById(dto.getAppointmentId())
                    .ifPresent(consultation::setAppointment);
        }

        // Si type = ZOOM → créer un meeting
        if (consultation.getType() == ConsultationType.ZOOM) {
            ZoomMeetingInfo meeting = zoomClient.createMeeting(
                    consultation.getDoctor().getId(),
                    "Consultation with " + consultation.getPatient().getFirstname(),
                    consultation.getDate(),
                    30
            );
            consultation.setZoomMeetingId(meeting.getId());
            consultation.setZoomJoinUrl(meeting.getJoinUrl());
            consultation.setZoomStartUrl(meeting.getStartUrl());
        }

        consultation.setStatus(ConsultationStatus.SCHEDULED);

        Consultation saved = repo.save(consultation);
        return mapper.toDto(saved);
    }


    @Transactional
    @Override
    public void cancelConsultation(Long id) {
        Consultation c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consultation not found"));

        if (c.getZoomMeetingId() != null) {
            zoomClient.deleteMeeting(c.getZoomMeetingId());
        }

        c.setStatus(ConsultationStatus.CANCELLED);
        repo.save(c);
    }

    @Override
    public ConsultationDTO getConsultation(Long id) {
        Consultation c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consultation not found"));
        return mapper.toDto(c);
    }
}
