package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.SessionDebt;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionDebtRepository extends JpaRepository<SessionDebt, Long> {

    /** Öğrencinin borçları, en eski ders önce. */
    @Query("""
            select d from SessionDebt d
              join fetch d.history h
              join h.lesson l
            where d.student.id = :studentId
            order by l.lessonDate, l.startTime, d.id
            """)
    List<SessionDebt> findByStudentIdOldestFirst(@Param("studentId") Long studentId);

    Optional<SessionDebt> findByHistoryId(Long historyId);

    /** Öğrenci başına borçlu seans sayısı: [studentId, count]. */
    @Query("""
            select d.student.id, count(d) from SessionDebt d
            where d.student.id in :studentIds
            group by d.student.id
            """)
    List<Object[]> countByStudentIds(@Param("studentIds") Collection<Long> studentIds);

    /** Seans borcu olan tekil öğrenci sayısı. */
    @Query("select count(distinct d.student.id) from SessionDebt d")
    long countDebtStudents();
}
