package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.model.Specialty;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", implementationName = "SpecialtyServiceMapperImpl")
public interface SpecialtyMapper {

    // mapping simple
    SpecialtyDto toDto(Specialty specialty);

    Specialty toEntity(SpecialtyDto dto);

    // mapping des listes
    List<SpecialtyDto> toDtoList(List<Specialty> specialties);

    List<Specialty> toEntityList(List<SpecialtyDto> specialtyDtos);

}