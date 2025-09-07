package com.telemedecine.api.service;

import com.telemedecine.api.dto.SpecialtyDto;

import java.util.List;

public interface SpecialtyService {

    List<SpecialtyDto> getAllSpecialties();

    SpecialtyDto getSpecialtyById(Long id);

    SpecialtyDto createSpecialty(SpecialtyDto dto);

    SpecialtyDto updateSpecialty(SpecialtyDto dto, Long id);

     void deleteSpecialty(Long id);

}
