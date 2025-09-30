package com.telemedecine.api.model.appointement;

import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.user.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @OneToOne
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private Slot slot;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

}
