package com.telemedecine.api.service;

import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.model.user.UserEntity;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {

     DoctorAvailabilityDTO createAvailability(DoctorAvailabilityDTO dto, UserEntity authenticatedUser);
     List<DoctorAvailabilityDTO> getDoctorAvailabilities(Long doctorId);
     List<DoctorAvailabilityDTO> getDoctorAvailabilitiesByDate(Long doctorId, LocalDate date);
     List<DoctorAvailabilityDTO> getDoctorAvailabilityOccurrences(
             Long doctorId, LocalDate from, LocalDate to, boolean includeSlots);
     void deleteAvailability(Long id, UserEntity authenticatedUser);


}
