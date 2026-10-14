package com.paze.pilates.service;

import com.paze.pilates.dto.SessionPackageDto;
import java.util.List;

public interface SessionPackageService {

    /** Aktif ve pasif tüm paketleri başlangıç tarihine göre (yeniden eskiye) döner. */
    List<SessionPackageDto> findAll();

    /** Yalnızca aktif paketleri ada göre döner. */
    List<SessionPackageDto> findActive();

    /**
     * Yeni paket oluşturur veya mevcut olanın tanım alanlarını günceller.
     * Aktiflik durumu burada değişmez; {@link #activate} / {@link #deactivate} kullanılır.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    SessionPackageDto save(SessionPackageDto dto);

    /** Paketi aktife alır; yeni atamalarda seçilebilir olur. */
    SessionPackageDto activate(Long id);

    /** Paketi pasife alır; mevcut atamalar etkilenmez, yeni atama yapılamaz. */
    SessionPackageDto deactivate(Long id);
}
