package com.telemedecine.api.service.impl;

import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.mapper.SlotMapper;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final SlotMapper slotMapper;

    @Override
    public SlotDto createSlot(SlotDto dto) {
        DoctorAvailability availability = availabilityRepository.findById(dto.getAvailabilityId())
                .orElseThrow(() -> new RuntimeException("Availability not found"));

        Slot slot = slotMapper.toEntity(dto);
        slot.setAvailability(availability);
        slot.setStatus(SlotStatus.FREE);

        slot = slotRepository.save(slot);
        return slotMapper.toDto(slot);
    }

    @Override
    public List<SlotDto> getSlotsByAvailability(Long availabilityId) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new RuntimeException("Availability not found"));

        return slotRepository.findByAvailability(availability).stream()
                .map(slotMapper::toDto)
                .toList();
    }

    @Override
    public SlotDto updateSlotStatus(Long slotId, SlotStatus status) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStatus(status);
        slot = slotRepository.save(slot);

        return slotMapper.toDto(slot);
    }
}
