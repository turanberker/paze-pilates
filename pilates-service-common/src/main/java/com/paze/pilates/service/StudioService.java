package com.paze.pilates.service;

import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.dto.StudioDto;
import com.paze.pilates.dto.StudioSettingsDto;
import java.util.List;
import java.util.Optional;

public interface StudioService {

    Optional<StudioDto> getStudio();

    /** Header'da gösterilecek stüdyo logosu; giriş yapmış her kullanıcı okuyabilir. */
    Optional<byte[]> getLogo();

    /** Stüdyo ayarları ekranı için ad ve logo. */
    StudioSettingsDto getSettings();

    /**
     * Stüdyo adını ve logosunu günceller; logo null ise mevcut logo kaldırılır.
     * Masaüstü sürümde tek stüdyo kaydı vardır, yenisi oluşturulmaz.
     */
    void updateSettings(StudioSettingsDto settings);

    List<RoomDto> getRooms();

    RoomDto saveRoom(RoomDto dto);

    void deleteRoom(Long id);
}
