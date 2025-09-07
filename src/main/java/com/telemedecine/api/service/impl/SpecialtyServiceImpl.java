package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.service.SpecialtyService;
import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.mapper.SpecialtyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    public List<SpecialtyDto> getAllSpecialties() {
        List<Specialty> specialties = specialtyRepository.findAll();
        return specialtyMapper.toDtoList(specialties);
    }

    @Override
    public SpecialtyDto getSpecialtyById(Long id) {
        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Specialty not found"));
        return specialtyMapper.toDto(specialty);
    }

    @Override
    public SpecialtyDto createSpecialty(SpecialtyDto dto) {
        Specialty specialty = specialtyMapper.toEntity(dto);
        Specialty saved = specialtyRepository.save(specialty);
        return specialtyMapper.toDto(saved);
    }

    @Override
    public SpecialtyDto updateSpecialty(SpecialtyDto dto, Long id) {
        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Specialty not found"));

        specialty.setName(dto.getName());
        specialty.setDescription(dto.getDescription());

        Specialty updated = specialtyRepository.save(specialty);
        return specialtyMapper.toDto(updated);
    }

    @Override
    public void deleteSpecialty(Long id) {
        Specialty specialty = specialtyRepository.findById(id).orElseThrow(() -> new RuntimeException("Specialty not found"));
        specialtyRepository.delete(specialty);
    }
}