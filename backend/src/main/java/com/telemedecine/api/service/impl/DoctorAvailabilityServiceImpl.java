package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.mapper.DoctorAvailabilityMapper;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.service.DoctorAvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private final DoctorAvailabilityRepository repository;
    private final DoctorRepository doctorRepository;
    private final SlotRepository slotRepository;
    private final DoctorAvailabilityMapper mapper;

    @Override
    @Transactional
    public DoctorAvailabilityDTO createAvailability(
            DoctorAvailabilityDTO dto,
            UserEntity authenticatedUser) {
        Long doctorId = authenticatedUser.getRole() == Role.DOCTOR
                ? authenticatedUser.getId()
                : dto.getDoctorId();
        if (doctorId == null) {
            throw new IllegalArgumentException("Doctor id is required");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        validate(dto);

        DoctorAvailability candidate = mapper.toEntity(dto);
        candidate.setId(null);
        candidate.setDoctor(doctor);
        candidate.setDate(dto.getType() == AvailabilityType.ONE_OFF ? dto.getDate() : null);
        candidate.setDayOfWeek(dto.getType() == AvailabilityType.RECURRING ? dto.getDayOfWeek() : null);
        candidate.setStartDate(dto.getType() == AvailabilityType.RECURRING ? dto.getStartDate() : null);
        candidate.setEndDate(dto.getType() == AvailabilityType.RECURRING ? dto.getEndDate() : null);

        repository.findByDoctorId(doctorId).stream()
                .filter(existing -> schedulesOverlap(existing, candidate))
                .findFirst()
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(String.format(
                            "Overlap detected with existing availability [%s - %s]",
                            existing.getStartTime(), existing.getEndTime()));
                });

        DoctorAvailability saved = repository.save(candidate);
        generateSlotsForAvailability(saved);
        return mapper.toDto(saved);
    }

    private void validate(DoctorAvailabilityDTO dto) {
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Availability type is required");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (dto.getSlotDuration() == null || dto.getSlotDuration() <= 0) {
            throw new IllegalArgumentException("Slot duration must be greater than zero");
        }
        if (dto.getType() == AvailabilityType.ONE_OFF && dto.getDate() == null) {
            throw new IllegalArgumentException("Date is required for one-off availability");
        }
        if (dto.getType() == AvailabilityType.RECURRING) {
            if (dto.getDayOfWeek() == null || dto.getStartDate() == null) {
                throw new IllegalArgumentException(
                        "Day of week and start date are required for recurring availability");
            }
            if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new IllegalArgumentException("Recurrence end date cannot be before start date");
            }
        }
    }

    private void generateSlotsForAvailability(DoctorAvailability availability) {
        int durationMinutes = availability.getSlotDuration();
        LocalTime current = availability.getStartTime();
        LocalTime end = availability.getEndTime();
        List<Slot> slots = new ArrayList<>();

        while (!current.plusMinutes(durationMinutes).isAfter(end)) {
            slots.add(Slot.builder()
                    .availability(availability)
                    .startTime(current)
                    .endTime(current.plusMinutes(durationMinutes))
                    .status(SlotStatus.FREE)
                    .build());
            current = current.plusMinutes(durationMinutes);
        }
        slotRepository.saveAll(slots);
    }

    private boolean schedulesOverlap(DoctorAvailability existing, DoctorAvailability candidate) {
        if (!timesOverlap(existing, candidate)) {
            return false;
        }
        if (existing.getType() == AvailabilityType.ONE_OFF
                && candidate.getType() == AvailabilityType.ONE_OFF) {
            return existing.getDate().equals(candidate.getDate());
        }
        if (existing.getType() == AvailabilityType.RECURRING
                && candidate.getType() == AvailabilityType.RECURRING) {
            return existing.getDayOfWeek() == candidate.getDayOfWeek()
                    && dateRangesOverlap(existing.getStartDate(), existing.getEndDate(),
                    candidate.getStartDate(), candidate.getEndDate());
        }

        DoctorAvailability oneOff = existing.getType() == AvailabilityType.ONE_OFF ? existing : candidate;
        DoctorAvailability recurring = existing.getType() == AvailabilityType.RECURRING ? existing : candidate;
        return recurringAppliesOn(recurring, oneOff.getDate());
    }

    private boolean timesOverlap(DoctorAvailability first, DoctorAvailability second) {
        return first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }

    private boolean dateRangesOverlap(
            LocalDate firstStart,
            LocalDate firstEnd,
            LocalDate secondStart,
            LocalDate secondEnd) {
        LocalDate effectiveFirstEnd = firstEnd == null ? LocalDate.MAX : firstEnd;
        LocalDate effectiveSecondEnd = secondEnd == null ? LocalDate.MAX : secondEnd;
        return !firstStart.isAfter(effectiveSecondEnd) && !secondStart.isAfter(effectiveFirstEnd);
    }

    private boolean recurringAppliesOn(DoctorAvailability recurring, LocalDate date) {
        return date != null
                && recurring.getDayOfWeek() == date.getDayOfWeek()
                && !date.isBefore(recurring.getStartDate())
                && (recurring.getEndDate() == null || !date.isAfter(recurring.getEndDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getDoctorAvailabilities(Long doctorId) {
        return repository.findByDoctorId(doctorId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getDoctorAvailabilitiesByDate(Long doctorId, LocalDate date) {
        return getDoctorAvailabilityOccurrences(doctorId, date, date, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getDoctorAvailabilityOccurrences(
            Long doctorId,
            LocalDate from,
            LocalDate to,
            boolean includeSlots) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        List<DoctorAvailability> rules = includeSlots
                ? repository.findByDoctorIdWithSlots(doctorId)
                : repository.findByDoctorId(doctorId);
        return rules.stream()
                .flatMap(rule -> expandRule(rule, from, to, includeSlots).stream())
                .sorted(Comparator.comparing(DoctorAvailabilityDTO::getDate)
                        .thenComparing(DoctorAvailabilityDTO::getStartTime)
                        .thenComparing(DoctorAvailabilityDTO::getId))
                .toList();
    }

    private List<DoctorAvailabilityDTO> expandRule(
            DoctorAvailability rule,
            LocalDate from,
            LocalDate to,
            boolean includeSlots) {
        if (rule.getType() == AvailabilityType.ONE_OFF) {
            if (rule.getDate() == null || rule.getDate().isBefore(from) || rule.getDate().isAfter(to)) {
                return List.of();
            }
            return List.of(toOccurrence(rule, rule.getDate(), includeSlots));
        }

        if (rule.getDayOfWeek() == null || rule.getStartDate() == null) {
            return List.of();
        }

        LocalDate effectiveFrom = from.isAfter(rule.getStartDate()) ? from : rule.getStartDate();
        LocalDate effectiveTo = rule.getEndDate() == null || to.isBefore(rule.getEndDate())
                ? to
                : rule.getEndDate();
        if (effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }

        DayOfWeek day = rule.getDayOfWeek();
        LocalDate occurrenceDate = effectiveFrom.with(TemporalAdjusters.nextOrSame(day));
        List<DoctorAvailabilityDTO> occurrences = new ArrayList<>();
        while (!occurrenceDate.isAfter(effectiveTo)) {
            occurrences.add(toOccurrence(rule, occurrenceDate, includeSlots));
            occurrenceDate = occurrenceDate.plusWeeks(1);
        }
        return occurrences;
    }

    private DoctorAvailabilityDTO toOccurrence(
            DoctorAvailability rule,
            LocalDate occurrenceDate,
            boolean includeSlots) {
        DoctorAvailabilityDTO occurrence = mapper.toDto(rule);
        occurrence.setDate(occurrenceDate);
        if (includeSlots) {
            occurrence.setSlots(rule.getSlots().stream()
                    .map(slot -> toSlotDto(slot, occurrenceDate))
                    .toList());
        }
        return occurrence;
    }

    private SlotDto toSlotDto(Slot slot, LocalDate occurrenceDate) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setAvailabilityId(slot.getAvailability().getId());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setStartDateTime(occurrenceDate.atTime(slot.getStartTime()));
        dto.setEndDateTime(occurrenceDate.atTime(slot.getEndTime()));
        dto.setStatus(slot.getStatus());
        return dto;
    }

    @Override
    @Transactional
    public void deleteAvailability(Long id, UserEntity authenticatedUser) {
        DoctorAvailability availability = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Availability not found"));
        if (authenticatedUser.getRole() != Role.ADMIN
                && !availability.getDoctor().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("Cannot delete another doctor's availability");
        }
        repository.delete(availability);
    }
}
