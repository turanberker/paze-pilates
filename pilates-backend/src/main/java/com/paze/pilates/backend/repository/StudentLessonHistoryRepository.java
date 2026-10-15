package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.StudentLessonHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentLessonHistoryRepository extends JpaRepository<StudentLessonHistory, Long> {

    @Query("""
            select h from StudentLessonHistory h
              left join fetch h.studentPackage sp
              left join fetch sp.sessionPackage
            where h.lesson.id = :lessonId
            """)
    List<StudentLessonHistory> findByLessonId(@Param("lessonId") Long lessonId);

    /** Günün tamamlanmış derslerinde gelen öğrenci kaydı sayısı. */
    @Query("""
            select count(h) from StudentLessonHistory h
            where h.lesson.lessonDate = :date and h.attended = true
            """)
    long countAttendedOn(@Param("date") LocalDate date);
}
