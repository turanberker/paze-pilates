package com.paze.pilates.service;

import com.paze.pilates.dto.ExpenseDto;
import com.paze.pilates.dto.MonthlyFinanceSummaryDto;
import java.time.YearMonth;
import java.util.List;

public interface ExpenseService {

    /** Verilen aydaki giderleri tarihe göre (yeniden eskiye) döner. */
    List<ExpenseDto> findByMonth(YearMonth month);

    /**
     * Yeni gider oluşturur veya mevcut olanın tarih, tutar ve açıklamasını günceller.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    ExpenseDto save(ExpenseDto dto);

    /** Gider kaydını siler. */
    void delete(Long id);

    /** Ayın geliri (ödemesi alınan paketler), gideri ve neti. */
    MonthlyFinanceSummaryDto getMonthlySummary(YearMonth month);
}
