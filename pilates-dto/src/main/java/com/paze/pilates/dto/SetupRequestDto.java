package com.paze.pilates.dto;

/** İlk kurulum formu: stüdyo adı ve ilk yöneticinin bilgileri. */
public record SetupRequestDto(
        String studioName,
        String adminFullName,
        String username,
        String password,
        String passwordConfirm
) {
}
