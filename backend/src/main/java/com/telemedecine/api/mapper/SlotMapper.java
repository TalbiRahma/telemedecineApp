package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.model.slot.Slot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SlotMapper {
    public SlotDto toDto(Slot slot) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setStatus(slot.getStatus());
        if (slot.getAvailability() != null) {
            dto.setAvailabilityId(slot.getAvailability().getId());
            if (slot.getAvailability().getDate() != null) {
                if (slot.getStartTime() != null) {
                    dto.setStartDateTime(LocalDateTime.of(slot.getAvailability().getDate(), slot.getStartTime()));
                }
                if (slot.getEndTime() != null) {
                    dto.setEndDateTime(LocalDateTime.of(slot.getAvailability().getDate(), slot.getEndTime()));
                }
            }
        }
        return dto;
    }

    public Slot toEntity(SlotDto dto) {
        Slot slot = new Slot();
        slot.setId(dto.getId());
        slot.setStartTime(dto.getStartTime() != null ? dto.getStartTime()
                : dto.getStartDateTime() == null ? null : dto.getStartDateTime().toLocalTime());
        slot.setEndTime(dto.getEndTime() != null ? dto.getEndTime()
                : dto.getEndDateTime() == null ? null : dto.getEndDateTime().toLocalTime());
        slot.setStatus(dto.getStatus());
        return slot;
    }
}
