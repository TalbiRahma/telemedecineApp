package com.telemedecine.api.service;

import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.dto.AdminOverviewStatsDto;
import com.telemedecine.api.model.user.doctor.DoctorState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public AdminOverviewStatsDto getOverviewStats() {
        return new AdminOverviewStatsDto(
                userRepository.count(),
                doctorRepository.count(),
                doctorRepository.countByState(DoctorState.PENDING),
                doctorRepository.countByState(DoctorState.CONFIRMED),
                doctorRepository.countByState(DoctorState.REJECTED),
                patientRepository.count(),
                appointmentRepository.count()
        );
    }
}
