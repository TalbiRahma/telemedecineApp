package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.mapper.AppointmentMapper;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.service.AppointmentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    public AppointmentDto bookAppointment(AppointmentDto dto) {
        Slot slot = slotRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new EntityNotFoundException("Slot not found"));

        if (slot.getStatus() != SlotStatus.FREE) {
            throw new IllegalStateException("Slot is not available");
        }

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        Appointment appointment = appointmentMapper.toEntity(dto);
        appointment.setSlot(slot);
        appointment.setPatient(patient);
        appointment.setBookedAt(LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.BOOKED);

        // update slot
        slot.setStatus(SlotStatus.PENDING_CONFIRMATION);

        appointment = appointmentRepository.save(appointment);
        return appointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentDto getAppointment(Long id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));
    }

    @Override
    public List<AppointmentDto> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(appointmentMapper::toDto)
                .toList();
    }

    @Override
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));

        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalStateException("Appointment already canceled");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointment.getSlot().setStatus(SlotStatus.CANCELLED);
    }

    @Override
    public AppointmentDto completeAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.getSlot().setStatus(SlotStatus.COMPLETED);

        return appointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentDto confirmAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only booked appointments can be confirmed");
        }

        // update statuses
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.getSlot().setStatus(SlotStatus.CONFIRMED);

        return appointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentDto markNoShow(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointment.getSlot().setStatus(SlotStatus.NO_SHOW);

        return appointmentMapper.toDto(appointment);
    }


}
