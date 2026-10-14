package com.paze.pilates.service;

import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.dto.StudioDto;
import java.util.List;
import java.util.Optional;

public interface StudioService {

    Optional<StudioDto> getStudio();

    /** Header'da gösterilecek stüdyo logosu; giriş yapmış her kullanıcı okuyabilir. */
    Optional<byte[]> getLogo();

    StudioDto saveStudio(StudioDto dto);

    List<RoomDto> getRooms();

    RoomDto saveRoom(RoomDto dto);

    void deleteRoom(Long id);
}
