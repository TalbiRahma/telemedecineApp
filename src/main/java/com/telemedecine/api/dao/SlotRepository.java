package com.telemedecine.api.dao;

import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByAvailability(DoctorAvailability availability);
}