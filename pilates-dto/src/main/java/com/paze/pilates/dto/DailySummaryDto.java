package com.paze.pilates.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Yönetici ana ekranındaki günlük özet.
 *
 * @param date                  özetin günü
 * @param lessonCount           o gün açılmış ders sayısı
 * @param completedLessonCount  o günün tamamlanmış ders sayısı
 * @param reservationCount      o günün derslerindeki toplam katılımcı kaydı
 * @param studentCount          o gün derse gelecek tekil öğrenci sayısı
 * @param attendedCount         tamamlanan derslerde gelen öğrenci kaydı
 * @param occupancyPercent      katılımcı / oda kapasitesi (0-100; ders yoksa 0)
 * @param collectedAmount       o gün ödemesi alınan paketlerin fiyat toplamı
 * @param collectedPackageCount o gün ödemesi alınan paket sayısı
 * @param expiringPackageCount  bitmek üzere olan (kalan seans az) paket sayısı
 * @param unpaidPackageCount    ödemesi alınmamış paket sayısı
 * @param unpaidAmount          ödemesi alınmamış paketlerin fiyat toplamı
 * @param debtStudentCount      seans borcu olan öğrenci sayısı
 * @param pendingLessonCount    günü geçmiş ama tamamlanmamış (yoklaması alınmamış) ders sayısı
 */
public record DailySummaryDto(
        LocalDate date,
        int lessonCount,
        int completedLessonCount,
        int reservationCount,
        int studentCount,
        int attendedCount,
        int occupancyPercent,
        BigDecimal collectedAmount,
        int collectedPackageCount,
        int expiringPackageCount,
        int unpaidPackageCount,
        BigDecimal unpaidAmount,
        int debtStudentCount,
        int pendingLessonCount
) {
}
