package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.model.Specialty;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {

    default SpecialtyDto toDto(Specialty specialty) {
        if (specialty == null) {
            return null;
        }

        return SpecialtyDto.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .createdAt(specialty.getCreatedAt())
                .updatedAt(specialty.getUpdatedAt())
                .build();
    }

    default Specialty toEntity(SpecialtyDto dto) {
        if (dto == null) {
            return null;
        }

        return Specialty.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                // createdAt and updatedAt are managed by Hibernate annotations
                .build();
    }

    default List<SpecialtyDto> toDtoList(List<Specialty> specialties) {
        if (specialties == null) {
            return null;
        }
        return specialties.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    default List<Specialty> toEntityList(List<SpecialtyDto> specialtyDtos) {
        if (specialtyDtos == null) {
            return null;
        }
        return specialtyDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}