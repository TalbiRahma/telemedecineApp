package com.telemedecine.api.service;

import com.telemedecine.api.dao.*;
import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.exception.SlotUnavailableException;
import com.telemedecine.api.mapper.AppointmentMapper;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock AppointmentRepository appointmentRepository;
    @Mock SlotRepository slotRepository;
    @Mock PatientRepository patientRepository;
    @Mock UserRepository userRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock DoctorAvailabilityService availabilityService;
    @Mock AppointmentMapper appointmentMapper;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(appointmentRepository, slotRepository, patientRepository,
                userRepository, doctorRepository, availabilityService, appointmentMapper);
    }

    @Test
    void booksConcreteRecurringOccurrenceForAuthenticatedPatient() {
        Patient authenticatedPatient = patient(9L, "patient@example.com");
        LocalDate occurrenceDate = futureMonday();
        Slot slot = recurringSlot(44L, occurrenceDate.minusWeeks(2));
        AppointmentDto request = new AppointmentDto();
        request.setSlotId(44L);
        request.setPatientId(999L);
        request.setSlotStartDateTime(occurrenceDate.atTime(9, 0));

        when(userRepository.findByEmail(authenticatedPatient.getEmail())).thenReturn(Optional.of(authenticatedPatient));
        when(patientRepository.findByEmail(authenticatedPatient.getEmail())).thenReturn(Optional.of(authenticatedPatient));
        when(slotRepository.findByIdForBooking(44L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotIdAndScheduledStartAndStatusIn(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentMapper.toDto(any())).thenReturn(new AppointmentDto());

        service.bookAppointment(request, authenticatedPatient.getEmail());

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(saved.capture());
        assertThat(saved.getValue().getPatient().getId()).isEqualTo(9L);
        assertThat(saved.getValue().getScheduledStart()).isEqualTo(occurrenceDate.atTime(9, 0));
        assertThat(saved.getValue().getScheduledEnd()).isEqualTo(occurrenceDate.atTime(9, 30));
        assertThat(saved.getValue().getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    void rejectsOccurrenceThatWasBookedWhilePatientWasReviewing() {
        Patient authenticatedPatient = patient(9L, "patient@example.com");
        LocalDate occurrenceDate = futureMonday();
        Slot slot = recurringSlot(44L, occurrenceDate.minusWeeks(2));
        AppointmentDto request = new AppointmentDto();
        request.setSlotId(44L);
        request.setSlotStartDateTime(occurrenceDate.atTime(9, 0));

        when(userRepository.findByEmail(authenticatedPatient.getEmail())).thenReturn(Optional.of(authenticatedPatient));
        when(slotRepository.findByIdForBooking(44L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotIdAndScheduledStartAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.bookAppointment(request, authenticatedPatient.getEmail()))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("no longer available");
        verify(appointmentRepository, never()).save(any());
    }

    private Slot recurringSlot(Long id, LocalDate startDate) {
        Doctor doctor = new Doctor();
        doctor.setId(3L);
        doctor.setState(DoctorState.CONFIRMED);
        DoctorAvailability availability = DoctorAvailability.builder()
                .id(7L).doctor(doctor).type(AvailabilityType.RECURRING)
                .dayOfWeek(DayOfWeek.MONDAY).startDate(startDate)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).slotDuration(30).build();
        return Slot.builder().id(id).availability(availability).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30)).status(SlotStatus.FREE).build();
    }

    private Patient patient(Long id, String email) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setEmail(email);
        patient.setRole(Role.PATIENT);
        return patient;
    }

    private LocalDate futureMonday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return monday.isAfter(LocalDate.now()) ? monday : monday.plusWeeks(1);
    }
}
