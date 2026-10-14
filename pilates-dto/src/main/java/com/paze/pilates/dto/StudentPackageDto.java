package com.paze.pilates.dto;

import java.time.LocalDate;

/**
 * Öğrenciye atanmış paket (liste satırı).
 *
 * @param id                atama kimliği
 * @param studentId         öğrenci kimliği
 * @param studentName       öğrenci ad soyad
 * @param packageId         paket kimliği
 * @param packageName       paket adı
 * @param totalSessions     atama anındaki toplam seans
 * @param usedSessions      kullanılan seans
 * @param remainingSessions kalan seans
 * @param paid              ödeme alındı mı
 * @param paymentDate       ödeme tarihi (ödenmediyse null)
 * @param assignedDate      atama tarihi
 */
public record StudentPackageDto(
        Long id,
        Long studentId,
        String studentName,
        Long packageId,
        String packageName,
        int totalSessions,
        int usedSessions,
        int remainingSessions,
        boolean paid,
        LocalDate paymentDate,
        LocalDate assignedDate
) {
}
