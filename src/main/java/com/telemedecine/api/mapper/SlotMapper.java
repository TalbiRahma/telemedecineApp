package com.telemedecine.api.mapper;

import com.telemedecine.api.dto.SlotDto;
import com.telemedecine.api.model.slot.Slot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    // Entity → DTO
    @Mapping(source = "availability.id", target = "availabilityId")
    SlotDto toDto(Slot slot);

    // DTO → Entity
    @Mapping(target = "availability", ignore = true)
    Slot toEntity(SlotDto dto);
}