package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.dto.DoctorDto;
import com.telemedecine.api.mapper.DoctorMapper;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.service.DoctorService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    public DoctorDto updateDoctor(Long id, DoctorDto doctorDto, boolean allowStateChange) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + id));
        if (doctorDto.getFirstname() != null) {
            doctor.setFirstname(doctorDto.getFirstname());
        }
        if (doctorDto.getLastname() != null) {
            doctor.setLastname(doctorDto.getLastname());
        }
        if (doctorDto.getEmail() != null) {
            doctor.setEmail(doctorDto.getEmail());
        }
        if (doctorDto.getPhone() != null) {
            doctor.setPhone(doctorDto.getPhone());
        }
        if (doctorDto.getLicenseNumber() != null) {
            doctor.setLicenseNumber(doctorDto.getLicenseNumber());
        }
        if (doctorDto.getAdresse() != null) {
            doctor.setAdresse(doctorDto.getAdresse());
        }
        if (doctorDto.getCertificationUrl() != null) {
            doctor.setCertificationUrl(doctorDto.getCertificationUrl());
        }
        if (allowStateChange && doctorDto.getState() != null) {
            doctor.setState(doctorDto.getState());
        }
        if (doctorDto.getSpecialty() != null) {
            doctor.setSpecialty(doctorMapper.toEntity(doctorDto).getSpecialty());
        }

        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    public void  deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new EntityNotFoundException("Doctor not found with id: " + id);
        }
        doctorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DoctorDto getById(Long id) {
        return doctorRepository.findById(id)
                .map(doctorMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> getAll() {
        return doctorMapper.toDtoList(doctorRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> getBookableDoctors() {
        return doctorMapper.toDtoList(doctorRepository.findByState(DoctorState.CONFIRMED));
    }

    @Transactional(readOnly = true)
    public DoctorDto getBookableDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .filter(candidate -> candidate.getState() == DoctorState.CONFIRMED)
                .orElseThrow(() -> new EntityNotFoundException("Bookable doctor not found with id: " + id));
        return doctorMapper.toDto(doctor);
    }
}
