package com.paze.pilates.service;

import com.paze.pilates.dto.AppUserDto;
import java.util.Optional;

public interface AuthService {

    /** Veritabanı kullanıcısı veya yapılandırmadaki sabit admin ile giriş yapar. */
    boolean authenticate(String username, String password);

    Optional<AppUserDto> getCurrentUser();

    void logout();

    /**
     * Oturumdaki kullanıcının kendi şifresini değiştirir; geçici şifre zorunluluğunu kaldırır.
     * Sabit admin şifresi uygulamadan değiştirilemez.
     *
     * @throws com.paze.pilates.dto.BusinessException eski şifre yanlışsa veya yeni şifre kurala uymuyorsa
     */
    void changeOwnPassword(String currentPassword, String newPassword, String newPasswordConfirm);
}
