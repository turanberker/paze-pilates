package com.paze.pilates.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seans paketi veri transfer nesnesi.
 *
 * @param id           null → yeni kayıt
 * @param name         paket adı (zorunlu, max 100)
 * @param price        ücret (zorunlu, 0 veya büyük)
 * @param sessionCount seans sayısı (zorunlu, en az 1)
 * @param startDate    paket başlangıç tarihi (zorunlu)
 * @param active       aktif/pasif bayrağı
 */
public record SessionPackageDto(
        Long id,
        String name,
        BigDecimal price,
        Integer sessionCount,
        LocalDate startDate,
        boolean active
) {
}
