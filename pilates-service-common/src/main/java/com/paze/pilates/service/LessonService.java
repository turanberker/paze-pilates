package com.paze.pilates.service;

import com.paze.pilates.dto.AttendanceDto;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.RoomDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface LessonService {

    /** İki tarih arasındaki (uçlar dahil) dersleri tarih ve saat sırasıyla döner. */
    List<LessonDto> findBetween(LocalDate from, LocalDate to);

    /**
     * Ana ekran için verilen günün dersleri, saat sırasıyla: yönetici tüm dersleri,
     * hoca yalnızca kendi derslerini görür.
     */
    List<LessonDto> findMyLessonsOn(LocalDate date);

    /** Ders açılabilecek hocalar: yönetici için tüm aktif hocalar, hoca için yalnızca kendisi. */
    List<InstructorDto> findInstructorsForLessons();

    /** Ders açılabilecek aktif odalar. */
    List<RoomDto> findRoomsForLessons();

    /**
     * Aktif hoca ve aktif odada tek seferlik ders açar. Hoca yalnızca kendi adına ders açabilir.
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
     * Paket bakiyesi bu aşamada düşülmez. Hoca yalnızca kendi dersine ekleyebilir.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama veya kural hatasında
     */
    LessonParticipantDto addParticipant(Long lessonId, Long studentId);

    /**
     * Öğrenciyi dersten çıkarır. Saati geçmiş dersten çıkarma yapılamaz. Hoca yalnızca kendi dersinden çıkarabilir.
     *
     * @throws com.paze.pilates.dto.BusinessException katılım yoksa veya ders bitmişse
     */
    void removeParticipant(Long participantId);

    /**
     * Dersi tamamlar: yoklamayı öğrenci ders geçmişine yazar ve seansı düşülecek her öğrencinin
     * paketinden bir seans düşer. Gelen öğrenciden her zaman, gelmeyenden hocanın kararıyla düşülür.
     * Paket sırası: önce ödemesi alınmış, sonra alınmamış; her grupta en eski atama.
     * Kalanı olan paketi yoksa seans borç olarak kaydedilir; paket atanınca düşülür.
     * Ders başlamadan veya ikinci kez tamamlanamaz; her katılımcı için yoklama verilmelidir.
     * Hoca yalnızca kendi dersini tamamlayabilir. Tamamlanan derse katılımcı eklenemez/çıkarılamaz.
     *
     * @throws com.paze.pilates.dto.BusinessException kural hatasında; hiçbir değişiklik kaydedilmez
     */
    LessonDto completeLesson(Long lessonId, List<AttendanceDto> attendance);

    /**
     * Tamamlamayı geri alır (yalnızca yönetici): düşülen seanslar paketlere iade edilir,
     * borç satırları ve ders geçmişi silinir, ders yeniden düzenlenebilir olur.
     *
     * @throws com.paze.pilates.dto.BusinessException ders yoksa veya tamamlanmamışsa
     */
    LessonDto undoCompletion(Long lessonId);
}
