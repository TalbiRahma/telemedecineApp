package com.telemedecine.api.service;

import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.dto.SlotDto;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentDto bookAppointment(AppointmentDto dto, String userEmail);
    AppointmentDto getAppointment(Long id, String userEmail);
    List<AppointmentDto> getAppointmentsByPatient(Long patientId, String userEmail);
    List<AppointmentDto> getAppointmentsByDoctor(Long doctorId, String userEmail);
    void cancelAppointment(Long id, String userEmail);
    AppointmentDto completeAppointment(Long id, String userEmail);
    AppointmentDto markNoShow(Long id, String userEmail);
    List<SlotDto> getBookableSlots(Long doctorId, LocalDate from, LocalDate to);
}
