package com.paze.pilates.service;

import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface LessonService {

    /** İki tarih arasındaki (uçlar dahil) dersleri tarih ve saat sırasıyla döner. */
    List<LessonDto> findBetween(LocalDate from, LocalDate to);

    /**
     * Aktif hoca ve aktif odada tek seferlik ders açar.
     * Aynı oda veya aynı hoca saat aralığı çakışan başka bir derste olamaz;
     * arka arkaya dersler (bitiş = başlangıç) çakışma sayılmaz.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama veya çakışma hatasında
     */
    LessonDto openLesson(Long instructorId, Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime);

    /** Dersin katılımcılarını soyadı / adı sırasıyla döner. */
    List<LessonParticipantDto> getParticipants(Long lessonId);

    /**
     * Aktif öğrenciyi derse ekler. Oda kapasitesi dolu olamaz, öğrenci derste zaten ekli
     * olamaz, aynı saat aralığında başka bir derste olamaz ve bitmiş derse ekleme yapılamaz.
     * Paket bakiyesi bu aşamada düşülmez.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama veya kural hatasında
     */
    LessonParticipantDto addParticipant(Long lessonId, Long studentId);

    /**
     * Öğrenciyi dersten çıkarır. Saati geçmiş dersten çıkarma yapılamaz.
     *
     * @throws com.paze.pilates.dto.BusinessException katılım yoksa veya ders bitmişse
     */
    void removeParticipant(Long participantId);
}
