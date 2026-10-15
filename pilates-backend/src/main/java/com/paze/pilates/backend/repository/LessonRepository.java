package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Lesson;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Tarih aralığındaki (uçlar dahil) dersler, tarih ve saat sırasıyla. */
    @Query("""
            select l from Lesson l
              join fetch l.instructor
              join fetch l.room
            where l.lessonDate between :from and :to
            order by l.lessonDate, l.startTime, l.id
            """)
    List<Lesson> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Aynı gün içinde verilen saat aralığıyla çakışan oda dersi var mı (bitiş = başlangıç çakışma sayılmaz). */
    @Query("""
            select count(l) > 0 from Lesson l
            where l.room.id = :roomId and l.lessonDate = :date
              and l.startTime < :end and l.endTime > :start
            """)
    boolean existsRoomOverlap(@Param("roomId") Long roomId, @Param("date") LocalDate date,
                              @Param("start") LocalTime start, @Param("end") LocalTime end);

    /** Aynı gün içinde verilen saat aralığıyla çakışan hoca dersi var mı. */
    @Query("""
            select count(l) > 0 from Lesson l
            where l.instructor.id = :instructorId and l.lessonDate = :date
              and l.startTime < :end and l.endTime > :start
            """)
    boolean existsInstructorOverlap(@Param("instructorId") Long instructorId, @Param("date") LocalDate date,
                                    @Param("start") LocalTime start, @Param("end") LocalTime end);

    /** Verilen günden önceki, tamamlanmamış ders sayısı. */
    long countByLessonDateBeforeAndCompletedAtIsNull(LocalDate date);
}
