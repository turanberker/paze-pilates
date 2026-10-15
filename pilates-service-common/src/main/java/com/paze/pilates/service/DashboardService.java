package com.paze.pilates.service;

import com.paze.pilates.dto.DailySummaryDto;
import java.time.LocalDate;

public interface DashboardService {

    /** Bitmek üzere sayılan paketin kalan seans üst sınırı (dahil). */
    int EXPIRING_REMAINING_THRESHOLD = 2;

    /**
     * Yönetici ana ekranı için günün özetini hesaplar. Gelir, o gün ödemesi alınan paketlerin
     * fiyat toplamıdır (ödeme tarihi = gün); ders başı bedel tutulmaz.
     *
     * @throws com.paze.pilates.dto.BusinessException yetki yoksa
     */
    DailySummaryDto getDailySummary(LocalDate date);
}
