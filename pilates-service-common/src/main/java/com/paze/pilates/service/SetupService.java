package com.paze.pilates.service;

import com.paze.pilates.dto.SetupRequestDto;

public interface SetupService {

    /** Veritabanında hiç kullanıcı yoksa true; giriş yerine kurulum ekranı gösterilir. */
    boolean isSetupRequired();

    /** Sabit admin yapılandırılmış mı (kurulum ekranındaki "sistem girişi" bağlantısı için). */
    boolean isStaticAdminAvailable();

    /**
     * Stüdyoyu ve ilk yöneticiyi tek işlemde oluşturur.
     *
     * @throws com.paze.pilates.dto.BusinessException kullanıcı zaten varsa veya alanlar geçersizse
     */
    void completeSetup(SetupRequestDto request);
}
