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
 * @param sessionDebt paketi olmadığı için düşülemeyen seans sayısı (yalnızca okuma; kayıtta dikkate alınmaz)
 */
public record StudentDto(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        LocalDate birthDate,
        String notes,
        boolean active,
        int sessionDebt
) {

    /** Form kaydı için: borç bilgisi servis tarafından hesaplanır. */
    public StudentDto(Long id, String firstName, String lastName, String phone, String email,
                      LocalDate birthDate, String notes, boolean active) {
        this(id, firstName, lastName, phone, email, birthDate, notes, active, 0);
    }
}
