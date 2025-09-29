package com.telemedecine.api.service;

import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.model.slot.SlotStatus;

import java.util.List;

public interface SlotService {

    SlotDto createSlot(SlotDto dto);
    List<SlotDto> getSlotsByAvailability(Long availabilityId);
    SlotDto updateSlotStatus(Long slotId, SlotStatus status);
}
