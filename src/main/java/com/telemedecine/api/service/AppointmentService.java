package com.telemedecine.api.service;

import com.telemedecine.api.dto.AppointmentDto;

import java.util.List;

public interface AppointmentService {

    AppointmentDto bookAppointment(AppointmentDto dto);
    AppointmentDto getAppointment(Long id);
    List<AppointmentDto> getAppointmentsByPatient(Long patientId);
    void cancelAppointment(Long id);
    AppointmentDto completeAppointment(Long id);
    AppointmentDto confirmAppointment(Long id);
    AppointmentDto markNoShow(Long id);
}
