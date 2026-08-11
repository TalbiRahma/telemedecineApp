package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.AppointmentDto;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {
    public AppointmentDto toDto(Appointment appointment) {
        Slot slot = appointment.getSlot();
        DoctorAvailability availability = slot == null ? null : slot.getAvailability();
        Doctor doctor = availability == null ? null : availability.getDoctor();
        Patient patient = appointment.getPatient();

        return AppointmentDto.builder()
                .id(appointment.getId())
                .bookedAt(appointment.getBookedAt())
                .status(appointment.getStatus())
                .slotId(slot == null ? null : slot.getId())
                .patientId(patient == null ? null : patient.getId())
                .slotStartDateTime(appointment.getScheduledStart())
                .slotEndDateTime(appointment.getScheduledEnd())
                .doctorId(doctor == null ? null : doctor.getId())
                .doctorFirstname(doctor == null ? null : doctor.getFirstname())
                .doctorLastname(doctor == null ? null : doctor.getLastname())
                .doctorSpecialtyName(doctor == null || doctor.getSpecialty() == null
                        ? null : doctor.getSpecialty().getName())
                .doctorAdresse(doctor == null ? null : doctor.getAdresse())
                .doctorPhone(doctor == null ? null : doctor.getPhone())
                .patientName(patient == null ? null
                        : (patient.getFirstname() + " " + patient.getLastname()).trim())
                .build();
    }
}
