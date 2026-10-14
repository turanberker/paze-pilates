package com.paze.pilates.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Takvimde açılmış ders.
 *
 * @param id             ders kimliği
 * @param instructorId   hoca kimliği
 * @param instructorName hoca ad soyad
 * @param roomId         oda kimliği
 * @param roomName       oda adı
 * @param date           ders günü
 * @param startTime      başlangıç saati
 * @param endTime        bitiş saati
 */
public record LessonDto(
        Long id,
        Long instructorId,
        String instructorName,
        Long roomId,
        String roomName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
}
