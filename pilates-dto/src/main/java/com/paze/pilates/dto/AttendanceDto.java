package com.paze.pilates.dto;

/**
 * Ders tamamlanırken bir katılımcının yoklaması.
 *
 * @param participantId katılım kaydı kimliği
 * @param attended      derse geldi mi
 * @param chargeNoShow  gelmediyse seansı yine de düşülsün mü (hocanın kararı); gelen öğrenciden her zaman düşülür
 */
public record AttendanceDto(Long participantId, boolean attended, boolean chargeNoShow) {

    public boolean charged() {
        return attended || chargeNoShow;
    }
}
