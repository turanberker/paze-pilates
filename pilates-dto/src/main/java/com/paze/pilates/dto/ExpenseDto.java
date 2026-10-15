package com.paze.pilates.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gider kaydı veri transfer nesnesi.
 *
 * @param id          null → yeni kayıt
 * @param expenseDate gider tarihi (zorunlu, ileri tarih olamaz)
 * @param amount      tutar (zorunlu, 0'dan büyük)
 * @param description açıklama (zorunlu, max 255)
 */
public record ExpenseDto(
        Long id,
        LocalDate expenseDate,
        BigDecimal amount,
        String description
) {
}
