package com.telemedecine.api.dao;

import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from Slot slot join fetch slot.availability availability join fetch availability.doctor where slot.id = :id")
    Optional<Slot> findByIdForBooking(@Param("id") Long id);
    List<Slot> findByAvailability(DoctorAvailability availability);
    @Query("SELECT s FROM Slot s WHERE s.availability.doctor.id = :doctorId AND s.status = :status AND s.availability.date BETWEEN :fromDate AND :toDate")
    List<Slot> findByDoctorIdAndStatusAndDateBetween(
            @Param("doctorId") Long doctorId,
            @Param("status") SlotStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
