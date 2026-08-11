package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.DoctorDto;
import com.telemedecine.api.model.user.doctor.Doctor;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class DoctorMapper {

    @Autowired
    protected SpecialtyMapper specialtyMapper;

    public DoctorDto toDto(Doctor doctor) {
        if (doctor == null) {
            return null;
        }

        return DoctorDto.builder()
                .id(doctor.getId())
                .firstname(doctor.getFirstname())
                .lastname(doctor.getLastname())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .licenseNumber(doctor.getLicenseNumber())
                .adresse(doctor.getAdresse())
                .certificationUrl(doctor.getCertificationUrl())
                .state(doctor.getState())
                .specialty(specialtyMapper.toDto(doctor.getSpecialty()))
                .build();
    }

    public Doctor toEntity(DoctorDto dto) {
        if (dto == null) {
            return null;
        }

        Doctor doctor = new Doctor();
        doctor.setId(dto.getId());
        doctor.setFirstname(dto.getFirstname());
        doctor.setLastname(dto.getLastname());
        doctor.setEmail(dto.getEmail());
        doctor.setPhone(dto.getPhone());
        doctor.setLicenseNumber(dto.getLicenseNumber());
        doctor.setAdresse(dto.getAdresse());
        doctor.setCertificationUrl(dto.getCertificationUrl());
        doctor.setState(dto.getState());
        doctor.setSpecialty(specialtyMapper.toEntity(dto.getSpecialty()));
        return doctor;
    }

    public List<DoctorDto> toDtoList(List<Doctor> doctors) {
        if (doctors == null) {
            return null;
        }
        return doctors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Doctor> toEntityList(List<DoctorDto> doctorDtos) {
        if (doctorDtos == null) {
            return null;
        }
        return doctorDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
