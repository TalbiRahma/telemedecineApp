package com.telemedecine.api.service;

import com.telemedecine.api.dto.DoctorAvailabilityDTO;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {

     DoctorAvailabilityDTO createAvailability(DoctorAvailabilityDTO dto);
     List<DoctorAvailabilityDTO> getDoctorAvailabilities(Long doctorId);
     List<DoctorAvailabilityDTO> getDoctorAvailabilitiesByDate(Long doctorId, LocalDate date);
     void deleteAvailability(Long id);


}
