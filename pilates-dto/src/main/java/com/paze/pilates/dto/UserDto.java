package com.paze.pilates.dto;

/** Kullanıcılar ekranındaki satır; şifre bilgisi içermez. */
public record UserDto(
        Long id,
        String username,
        String fullName,
        UserRole role,
        Long instructorId,
        String instructorName,
        boolean active,
        boolean mustChangePassword
) {
}
