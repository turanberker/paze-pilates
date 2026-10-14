package com.paze.pilates.service;

import com.paze.pilates.dto.SessionPackageDto;
import java.util.List;

public interface SessionPackageService {

    /** Aktif ve pasif tüm paketleri başlangıç tarihine göre (yeniden eskiye) döner. */
    List<SessionPackageDto> findAll();

    /** Yalnızca aktif paketleri ada göre döner. */
    List<SessionPackageDto> findActive();

    /**
     * Yeni paket oluşturur veya mevcut olanı günceller.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    SessionPackageDto save(SessionPackageDto dto);
}
