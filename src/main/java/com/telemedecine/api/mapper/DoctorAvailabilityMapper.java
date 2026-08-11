package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.DoctorAvailabilityDTO;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DoctorAvailabilityMapper {

    @Mapping(target = "doctorId", expression = "java(entity.getDoctor() != null ? entity.getDoctor().getId() : null)")
    @Mapping(target = "slotDuration", source = "slotDuration")
    @Mapping(target = "slots", ignore = true)
    DoctorAvailabilityDTO toDto(DoctorAvailability entity);

    @Mapping(target = "doctor", expression = "java(mapDoctorId(dto.getDoctorId()))")
    @Mapping(target = "slotDuration", source = "slotDuration")
    @Mapping(target = "slots", ignore = true)
    DoctorAvailability toEntity(DoctorAvailabilityDTO dto);

    default Doctor mapDoctorId(Long doctorId) {
        if (doctorId == null) return null;
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        return doctor;
    }


}
