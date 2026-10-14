package com.paze.pilates.dto;

import java.util.Set;

/**
 * Oturum açmış kullanıcı.
 *
 * @param id                 veritabanı kimliği; sabit adminde null
 * @param instructorId       hoca rolündeki kullanıcının bağlı olduğu hoca kaydı
 * @param mustChangePassword geçici şifreyle girdi; şifre değiştirene kadar işlem yapamaz
 * @param staticAdmin        yapılandırmadan gelen sabit admin
 * @param permissions        rolün yetkileri (ekran yalnızca menü/buton gizlemek için kullanır)
 */
public record AppUserDto(
        Long id,
        String username,
        String fullName,
        UserRole role,
        Long instructorId,
        boolean mustChangePassword,
        boolean staticAdmin,
        Set<Permission> permissions
) {
    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}
