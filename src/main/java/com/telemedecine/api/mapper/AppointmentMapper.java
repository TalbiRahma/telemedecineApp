package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.user.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mappings({
            @Mapping(source = "slot.id", target = "slotId"),
            @Mapping(source = "patient.id", target = "patientId")
    })
    AppointmentDto toDto(Appointment appointment);

    @Mappings({
            @Mapping(source = "slotId", target = "slot.id"),
            @Mapping(source = "patientId", target = "patient")
    })
    Appointment toEntity(AppointmentDto dto);

    default Patient map(Long patientId) {
        if (patientId == null) return null;
        Patient patient = new Patient();
        patient.setId(patientId);
        return patient;
    }
}
