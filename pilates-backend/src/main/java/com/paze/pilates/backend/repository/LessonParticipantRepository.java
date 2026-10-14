package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.LessonParticipant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonParticipantRepository extends JpaRepository<LessonParticipant, Long> {

    @Query("""
            select p from LessonParticipant p
              join fetch p.student s
            where p.lesson.id = :lessonId
            order by s.lastName, s.firstName
            """)
    List<LessonParticipant> findByLessonId(@Param("lessonId") Long lessonId);

    long countByLessonId(Long lessonId);

    boolean existsByLessonIdAndStudentId(Long lessonId, Long studentId);

    /** Verilen derslerin katılımcı sayıları: [lessonId, count]. */
    @Query("""
            select p.lesson.id, count(p) from LessonParticipant p
            where p.lesson.id in :lessonIds
            group by p.lesson.id
            """)
    List<Object[]> countByLessonIds(@Param("lessonIds") Collection<Long> lessonIds);

    /** Öğrenci aynı gün verilen saat aralığıyla çakışan başka bir derste mi. */
    @Query("""
            select count(p) > 0 from LessonParticipant p
            where p.student.id = :studentId and p.lesson.lessonDate = :date
              and p.lesson.startTime < :end and p.lesson.endTime > :start
            """)
    boolean existsStudentOverlap(@Param("studentId") Long studentId, @Param("date") LocalDate date,
                                 @Param("start") LocalTime start, @Param("end") LocalTime end);
}
