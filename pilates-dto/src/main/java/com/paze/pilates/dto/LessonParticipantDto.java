package com.paze.pilates.dto;

/**
 * Derse eklenmiş öğrenci.
 *
 * @param id          katılım kaydı kimliği
 * @param lessonId    ders kimliği
 * @param studentId   öğrenci kimliği
 * @param studentName öğrenci ad soyad
 * @param phone       öğrenci telefonu
 */
public record LessonParticipantDto(Long id, Long lessonId, Long studentId, String studentName, String phone) {
}
