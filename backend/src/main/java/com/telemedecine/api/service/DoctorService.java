package com.telemedecine.api.service;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dto.DoctorDto;
import com.telemedecine.api.mapper.DoctorMapper;
import com.telemedecine.api.model.user.doctor.Doctor;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DoctorService {

    DoctorDto updateDoctor(Long id, DoctorDto doctorDto, boolean allowStateChange);

    void  deleteDoctor(Long id);

    DoctorDto getById(Long id);

    List<DoctorDto> getAll();

    List<DoctorDto> getBookableDoctors();

    DoctorDto getBookableDoctor(Long id);

}
