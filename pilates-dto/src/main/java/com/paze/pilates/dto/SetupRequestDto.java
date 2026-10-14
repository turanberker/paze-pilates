package com.paze.pilates.dto;

/**
 * İlk kurulum formu: stüdyo adı, isteğe bağlı header logosu ve ilk yöneticinin bilgileri.
 *
 * @param logo PNG veya JPEG dosya içeriği; logo seçilmediyse null
 */
public record SetupRequestDto(
        String studioName,
        String adminFullName,
        String username,
        String password,
        String passwordConfirm,
        byte[] logo
) {

    /** Header logosu için izin verilen en büyük dosya boyutu (bayt). */
    public static final int LOGO_MAX_BYTES = 512 * 1024;
}
