package com.telemedecine.api.service;

import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentStatusScheduler {
    private final AppointmentRepository appointmentRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void updatePastAppointments() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> booked = appointmentRepository.findByStatus(AppointmentStatus.BOOKED);

        for (Appointment appointment : booked) {
            LocalDateTime end = appointment.getScheduledEnd();
            if (end != null && end.isBefore(now)) {
                appointment.setStatus(AppointmentStatus.COMPLETED);
            }
        }
    }
}
