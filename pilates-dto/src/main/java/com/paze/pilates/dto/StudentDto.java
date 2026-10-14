package com.paze.pilates.dto;

import java.time.LocalDate;

/**
 * Öğrenci veri transfer nesnesi.
 *
 * @param id        null → yeni kayıt
 * @param firstName ad (zorunlu)
 * @param lastName  soyad (zorunlu)
 * @param phone     telefon (zorunlu, max 20)
 * @param email     e-posta (isteğe bağlı, max 100)
 * @param birthDate doğum tarihi (isteğe bağlı)
 * @param notes     notlar / sağlık kısıtlamaları (isteğe bağlı, max 500)
 * @param active    aktif/pasif bayrağı
 */
public record StudentDto(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        LocalDate birthDate,
        String notes,
        boolean active
) {
}
