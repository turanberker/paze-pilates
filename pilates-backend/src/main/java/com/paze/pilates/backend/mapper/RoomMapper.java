package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.dto.RoomDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    RoomDto toDto(Room room);
}
