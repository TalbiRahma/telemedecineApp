package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.mapper.DoctorAvailabilityMapper;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private final DoctorAvailabilityRepository repository;
    private final DoctorAvailabilityMapper mapper;


    @Override
    public DoctorAvailabilityDTO createAvailability(DoctorAvailabilityDTO dto) {
        DoctorAvailability newAvailability = mapper.toEntity(dto);

        List<DoctorAvailability> sameDayAvailabilities = repository.findByDoctorIdAndDate(dto.getDoctorId(), dto.getDate());

        for (DoctorAvailability existing : sameDayAvailabilities) {
            if ( isOverlapping(existing, newAvailability)){
                throw new IllegalArgumentException(
                        String.format("Overlap detected with existing availability [%s - %s]",
                                existing.getStartTime(), existing.getEndTime()));
            }
        }
        return mapper.toDto(repository.save(newAvailability));
    }

    private boolean isOverlapping(DoctorAvailability existing, DoctorAvailability candidate){
        return !(candidate.getEndTime().isBefore(existing.getStartTime()) ||
                candidate.getStartTime().isAfter(existing.getEndTime()));
    }

    @Override
    public List<DoctorAvailabilityDTO> getDoctorAvailabilities(Long doctorId) {
        return repository.findByDoctorId(doctorId).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<DoctorAvailabilityDTO> getDoctorAvailabilitiesByDate(Long doctorId, LocalDate date) {
        return repository.findByDoctorIdAndDate(doctorId, date)
                .stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteAvailability(Long id) {
        repository.deleteById(id);
    }
}
