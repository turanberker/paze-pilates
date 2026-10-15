package com.paze.pilates.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Aylık gelir/gider özeti.
 *
 * @param month              özetin ayı
 * @param income             ay içinde ödemesi alınan paketlerin fiyat toplamı
 * @param incomePackageCount ay içinde ödemesi alınan paket sayısı
 * @param expense            ay içindeki giderlerin toplamı
 * @param expenseCount       ay içindeki gider kaydı sayısı
 * @param net                gelir - gider
 */
public record MonthlyFinanceSummaryDto(
        YearMonth month,
        BigDecimal income,
        int incomePackageCount,
        BigDecimal expense,
        int expenseCount,
        BigDecimal net
) {
}
