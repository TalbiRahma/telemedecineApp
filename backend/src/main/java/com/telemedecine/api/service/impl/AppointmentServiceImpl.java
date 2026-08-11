package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.exception.SlotUnavailableException;
import com.telemedecine.api.mapper.AppointmentMapper;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.service.AppointmentService;
import com.telemedecine.api.service.DoctorAvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityService availabilityService;
    private final AppointmentMapper appointmentMapper;

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED);

    @Override
    public AppointmentDto bookAppointment(AppointmentDto dto, String userEmail) {
        UserEntity user = requireUser(userEmail);
        if (user.getRole() != Role.PATIENT) throw new AccessDeniedException("Only patients can book appointments");

        if (dto.getSlotId() == null || dto.getSlotStartDateTime() == null) {
            throw new IllegalArgumentException("A concrete appointment time is required");
        }

        Slot slot = slotRepository.findByIdForBooking(dto.getSlotId())
                .orElseThrow(() -> new EntityNotFoundException("Slot not found"));
        LocalDateTime scheduledStart = dto.getSlotStartDateTime();
        validateBookableOccurrence(slot, scheduledStart);

        if (appointmentRepository.existsBySlotIdAndScheduledStartAndStatusIn(
                slot.getId(), scheduledStart, ACTIVE_STATUSES)) {
            throw new SlotUnavailableException(
                    "This time slot is no longer available. Please choose another time.");
        }

        Patient patient = patientRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
        Appointment appointment = Appointment.builder()
                .slot(slot)
                .patient(patient)
                .bookedAt(LocalDateTime.now())
                .scheduledStart(scheduledStart)
                .scheduledEnd(scheduledStart.toLocalDate().atTime(slot.getEndTime()))
                .status(AppointmentStatus.BOOKED)
                .build();
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Override
    public List<SlotDto> getBookableSlots(Long doctorId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (from == null || to == null || from.isBefore(today) || from.isAfter(to)) {
            throw new IllegalArgumentException("Choose a valid date range starting today or later");
        }
        if (from.plusDays(31).isBefore(to)) {
            throw new IllegalArgumentException("Availability can be requested for at most 31 days");
        }

        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        if (doctor.getState() != DoctorState.CONFIRMED) {
            throw new AccessDeniedException("This doctor is not available for patient booking");
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.atTime(LocalTime.MAX);
        Set<String> occupied = new HashSet<>();
        appointmentRepository
                .findBySlotAvailabilityDoctorIdAndScheduledStartBetweenAndStatusIn(
                        doctorId, rangeStart, rangeEnd, ACTIVE_STATUSES)
                .forEach(appointment -> occupied.add(
                        occurrenceKey(appointment.getSlot().getId(), appointment.getScheduledStart())));

        LocalDateTime now = LocalDateTime.now();
        return availabilityService.getDoctorAvailabilityOccurrences(doctorId, from, to, true).stream()
                .filter(occurrence -> occurrence.getSlots() != null)
                .flatMap(occurrence -> occurrence.getSlots().stream())
                .filter(slot -> slot.getId() != null && slot.getStartDateTime() != null)
                .filter(slot -> slot.getStatus() == SlotStatus.FREE)
                .filter(slot -> slot.getStartDateTime().isAfter(now))
                .filter(slot -> !occupied.contains(occurrenceKey(slot.getId(), slot.getStartDateTime())))
                .sorted(Comparator.comparing(SlotDto::getStartDateTime))
                .toList();
    }

    @Override
    public AppointmentDto getAppointment(Long id, String userEmail) {
        Appointment appointment = findAppointment(id);
        assertCanView(appointment, requireUser(userEmail));
        return appointmentMapper.toDto(appointment);
    }

    @Override
    public List<AppointmentDto> getAppointmentsByPatient(Long patientId, String userEmail) {
        UserEntity user = requireUser(userEmail);
        if (user.getRole() != Role.ADMIN && (user.getRole() != Role.PATIENT || !user.getId().equals(patientId))) {
            throw new AccessDeniedException("Cannot view another patient's appointments");
        }
        return appointmentRepository.findByPatientId(patientId).stream().map(appointmentMapper::toDto).toList();
    }

    @Override
    public List<AppointmentDto> getAppointmentsByDoctor(Long doctorId, String userEmail) {
        UserEntity user = requireUser(userEmail);
        if (user.getRole() != Role.ADMIN && (user.getRole() != Role.DOCTOR || !user.getId().equals(doctorId))) {
            throw new AccessDeniedException("Cannot view another doctor's appointments");
        }
        return appointmentRepository.findBySlotAvailabilityDoctorId(doctorId).stream()
                .map(appointmentMapper::toDto).toList();
    }

    @Override
    public void cancelAppointment(Long id, String userEmail) {
        Appointment appointment = findAppointment(id);
        assertCanView(appointment, requireUser(userEmail));
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalStateException("Appointment already canceled");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed appointment");
        }
        appointment.setStatus(AppointmentStatus.CANCELED);
    }

    @Override
    public AppointmentDto completeAppointment(Long id, String userEmail) {
        Appointment appointment = findAppointment(id);
        assertDoctorOwnsOrAdmin(appointment, requireUser(userEmail));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentDto markNoShow(Long id, String userEmail) {
        Appointment appointment = findAppointment(id);
        assertDoctorOwnsOrAdmin(appointment, requireUser(userEmail));
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return appointmentMapper.toDto(appointment);
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private void assertCanView(Appointment appointment, UserEntity user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.PATIENT && appointment.getPatient().getId().equals(user.getId())) return;
        if (user.getRole() == Role.DOCTOR && doctorId(appointment).equals(user.getId())) return;
        throw new AccessDeniedException("Appointment does not belong to the authenticated user");
    }

    private void assertDoctorOwnsOrAdmin(Appointment appointment, UserEntity user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.DOCTOR && doctorId(appointment).equals(user.getId())) return;
        throw new AccessDeniedException("Appointment does not belong to the authenticated doctor");
    }

    private Long doctorId(Appointment appointment) {
        return appointment.getSlot().getAvailability().getDoctor().getId();
    }

    private void validateBookableOccurrence(Slot slot, LocalDateTime scheduledStart) {
        var availability = slot.getAvailability();
        var doctor = availability.getDoctor();
        if (doctor.getState() != DoctorState.CONFIRMED) {
            throw new AccessDeniedException("This doctor is not available for patient booking");
        }
        if (slot.getStatus() != SlotStatus.FREE || scheduledStart.isBefore(LocalDateTime.now())) {
            throw new SlotUnavailableException(
                    "This time slot is no longer available. Please choose another time.");
        }
        if (!scheduledStart.toLocalTime().equals(slot.getStartTime())
                || !availabilityAppliesOn(availability, scheduledStart.toLocalDate())) {
            throw new IllegalArgumentException("The selected time does not belong to this doctor's availability");
        }
    }

    private boolean availabilityAppliesOn(
            com.telemedecine.api.model.user.doctor.DoctorAvailability availability,
            LocalDate date) {
        if (availability.getType() == AvailabilityType.ONE_OFF) {
            return date.equals(availability.getDate());
        }
        return availability.getType() == AvailabilityType.RECURRING
                && availability.getDayOfWeek() == date.getDayOfWeek()
                && availability.getStartDate() != null
                && !date.isBefore(availability.getStartDate())
                && (availability.getEndDate() == null || !date.isAfter(availability.getEndDate()));
    }

    private String occurrenceKey(Long slotId, LocalDateTime start) {
        return slotId + "@" + start;
    }
}
