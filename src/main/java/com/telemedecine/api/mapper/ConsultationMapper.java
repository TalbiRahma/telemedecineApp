package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.ConsultationDTO;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.consultation.Consultation;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.doctor.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ConsultationMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "doctor.id", target = "doctorId")
    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "patient.firstname", target = "patientName")
    @Mapping(source = "doctor.firstname", target = "doctorName")
    ConsultationDTO toDto(Consultation consultation);

    @Mapping(source = "patientId", target = "patient", qualifiedByName = "mapPatient")
    @Mapping(source = "doctorId", target = "doctor", qualifiedByName = "mapDoctor")
    @Mapping(source = "appointmentId", target = "appointment", qualifiedByName = "mapAppointment")
    Consultation toEntity(ConsultationDTO dto);

    //helpers
    @Named("mapPatient")
    default Patient mapPatient(Long id) {
        if (id == null) return null;
        Patient p = new Patient();
        p.setId(id);
        return p;
    }

    @Named("mapDoctor")
    default Doctor mapDoctor(Long id) {
        if (id == null) return null;
        Doctor d = new Doctor();
        d.setId(id);
        return d;
    }

    @Named("mapAppointment")
    default Appointment mapAppointment(Long id) {
        if (id == null) return null;
        Appointment a = new Appointment();
        a.setId(id);
        return a;
    }
}
