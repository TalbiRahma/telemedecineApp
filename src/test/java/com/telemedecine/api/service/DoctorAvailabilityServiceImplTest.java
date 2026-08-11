package com.telemedecine.api.service;

import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.mapper.DoctorAvailabilityMapper;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.service.impl.DoctorAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceImplTest {
    @Mock private DoctorAvailabilityRepository repository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private DoctorAvailabilityMapper mapper;

    private DoctorAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DoctorAvailabilityServiceImpl(
                repository, doctorRepository, slotRepository, mapper);
        lenient().when(mapper.toDto(any(DoctorAvailability.class))).thenAnswer(invocation -> {
            DoctorAvailability rule = invocation.getArgument(0);
            DoctorAvailabilityDTO dto = new DoctorAvailabilityDTO();
            dto.setId(rule.getId());
            dto.setDoctorId(rule.getDoctor().getId());
            dto.setDate(rule.getDate());
            dto.setDayOfWeek(rule.getDayOfWeek());
            dto.setStartDate(rule.getStartDate());
            dto.setEndDate(rule.getEndDate());
            dto.setStartTime(rule.getStartTime());
            dto.setEndTime(rule.getEndTime());
            dto.setType(rule.getType());
            dto.setSlotDuration(rule.getSlotDuration());
            return dto;
        });
    }

    @Test
    void expandsRecurringRuleInsideInclusiveWeek() {
        DoctorAvailability recurring = recurringRule(
                1L, DayOfWeek.MONDAY, LocalDate.of(2026, 8, 3), null);
        when(repository.findByDoctorId(17L)).thenReturn(List.of(recurring));

        List<DoctorAvailabilityDTO> occurrences = service.getDoctorAvailabilityOccurrences(
                17L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9), false);

        assertThat(occurrences).singleElement()
                .extracting(DoctorAvailabilityDTO::getDate)
                .isEqualTo(LocalDate.of(2026, 8, 3));
        verify(repository, never()).findByDoctorIdWithSlots(any());
    }

    @Test
    void respectsRecurringStartAndEndDatesWithoutDuplicates() {
        DoctorAvailability recurring = recurringRule(
                1L, DayOfWeek.MONDAY,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 17));
        when(repository.findByDoctorId(17L)).thenReturn(List.of(recurring));

        List<DoctorAvailabilityDTO> occurrences = service.getDoctorAvailabilityOccurrences(
                17L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 24), false);

        assertThat(occurrences).extracting(DoctorAvailabilityDTO::getDate)
                .containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 17));
    }

    @Test
    void returnsEmptyListWhenNoRulesExist() {
        when(repository.findByDoctorId(17L)).thenReturn(List.of());

        assertThat(service.getDoctorAvailabilityOccurrences(
                17L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9), false))
                .isEmpty();
    }

    @Test
    void doctorCannotDeleteAnotherDoctorsAvailability() {
        Doctor owner = doctor(18L);
        Doctor authenticatedDoctor = doctor(17L);
        DoctorAvailability availability = recurringRule(
                1L, DayOfWeek.MONDAY, LocalDate.of(2026, 8, 3), null);
        availability.setDoctor(owner);
        when(repository.findById(1L)).thenReturn(Optional.of(availability));

        assertThatThrownBy(() -> service.deleteAvailability(1L, authenticatedDoctor))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void doctorCanDeleteOwnAvailability() {
        Doctor authenticatedDoctor = doctor(17L);
        DoctorAvailability availability = recurringRule(
                1L, DayOfWeek.MONDAY, LocalDate.of(2026, 8, 3), null);
        when(repository.findById(1L)).thenReturn(Optional.of(availability));

        service.deleteAvailability(1L, authenticatedDoctor);

        verify(repository).delete(availability);
    }

    @Test
    void doctorIdInCreatePayloadCannotOverrideAuthenticatedDoctor() {
        Doctor authenticatedDoctor = doctor(17L);
        DoctorAvailabilityDTO request = new DoctorAvailabilityDTO();
        request.setDoctorId(18L);
        request.setType(AvailabilityType.RECURRING);
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setSlotDuration(30);
        DoctorAvailability mapped = new DoctorAvailability();
        mapped.setStartTime(request.getStartTime());
        mapped.setEndTime(request.getEndTime());
        mapped.setSlotDuration(request.getSlotDuration());
        mapped.setType(request.getType());
        mapped.setDayOfWeek(request.getDayOfWeek());
        mapped.setStartDate(request.getStartDate());

        when(doctorRepository.findById(17L)).thenReturn(Optional.of(authenticatedDoctor));
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(repository.findByDoctorId(17L)).thenReturn(List.of());
        when(repository.save(mapped)).thenReturn(mapped);

        service.createAvailability(request, authenticatedDoctor);

        verify(doctorRepository).findById(17L);
        assertThat(mapped.getDoctor()).isSameAs(authenticatedDoctor);
    }

    private DoctorAvailability recurringRule(
            Long id,
            DayOfWeek day,
            LocalDate startDate,
            LocalDate endDate) {
        return DoctorAvailability.builder()
                .id(id)
                .doctor(doctor(17L))
                .type(AvailabilityType.RECURRING)
                .dayOfWeek(day)
                .startDate(startDate)
                .endDate(endDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDuration(30)
                .build();
    }

    private Doctor doctor(Long id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setRole(Role.DOCTOR);
        return doctor;
    }
}
