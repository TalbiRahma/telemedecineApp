package com.telemedecine.api.service;

import com.telemedecine.api.dto.PatientDto;

import java.util.List;

public interface PatientService {
    PatientDto getCurrentPatient(String email);
    PatientDto updateCurrentPatient(String email, PatientDto dto);
    PatientDto getById(Long id);
    List<PatientDto> getAll();
}
