package com.paze.pilates.backend.security;

import com.paze.pilates.dto.UserRole;

/**
 * Oturumdaki kullanıcının servis katmanındaki özeti.
 *
 * @param id          sabit adminde null
 * @param instructorId hoca rolünde bağlı hoca kaydı
 */
public record CurrentUser(Long id, String username, String fullName, UserRole role, Long instructorId,
                          boolean staticAdmin, boolean mustChangePassword) {

    public CurrentUser withPasswordChanged() {
        return new CurrentUser(id, username, fullName, role, instructorId, staticAdmin, false);
    }
}
