package com.telemedecine.api.service;

import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.dto.AdminOverviewStatsDto;
import com.telemedecine.api.model.user.doctor.DoctorState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {
    @Test
    void overviewUsesRepositoryCounts() {
        UserRepository users = mock(UserRepository.class);
        DoctorRepository doctors = mock(DoctorRepository.class);
        PatientRepository patients = mock(PatientRepository.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        when(users.count()).thenReturn(9L);
        when(doctors.count()).thenReturn(5L);
        when(doctors.countByState(DoctorState.PENDING)).thenReturn(2L);
        when(doctors.countByState(DoctorState.CONFIRMED)).thenReturn(2L);
        when(doctors.countByState(DoctorState.REJECTED)).thenReturn(1L);
        when(patients.count()).thenReturn(3L);
        when(appointments.count()).thenReturn(12L);

        AdminOverviewStatsDto result = new AdminDashboardService(
                users, doctors, patients, appointments).getOverviewStats();

        assertThat(result).isEqualTo(new AdminOverviewStatsDto(9, 5, 2, 2, 1, 3, 12));
    }
}
