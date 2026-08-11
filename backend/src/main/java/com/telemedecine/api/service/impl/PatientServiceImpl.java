package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dto.PatientDto;
import com.telemedecine.api.mapper.PatientMapper;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.service.PatientService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional(readOnly = true)
    public PatientDto getCurrentPatient(String email) {
        return patientMapper.toDto(findByEmail(email));
    }

    @Override
    public PatientDto updateCurrentPatient(String email, PatientDto dto) {
        Patient patient = findByEmail(email);
        if (dto.getFirstname() != null) patient.setFirstname(dto.getFirstname());
        if (dto.getLastname() != null) patient.setLastname(dto.getLastname());
        if (dto.getPhone() != null) patient.setPhone(dto.getPhone());
        if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) patient.setGender(dto.getGender());
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDto getById(Long id) {
        return patientRepository.findById(id)
                .map(patientMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> getAll() {
        return patientRepository.findAll().stream().map(patientMapper::toDto).toList();
    }

    private Patient findByEmail(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
    }
}
