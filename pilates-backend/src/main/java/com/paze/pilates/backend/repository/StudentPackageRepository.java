package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.StudentPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, Long> {

    /**
     * Öğrenci adına ve kalan seans durumuna göre atamaları yeniden eskiye döner.
     *
     * @param term          ad/soyad içinde aranacak metin ("" → hepsi)
     * @param onlyRemaining true → yalnızca seansı kalanlar
     * @param onlyFinished  true → yalnızca seansı bitenler
     */
    @Query("""
            select sp from StudentPackage sp
              join fetch sp.student s
              join fetch sp.sessionPackage p
            where lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :term, '%'))
              and (:onlyRemaining = false or sp.usedSessions < sp.totalSessions)
              and (:onlyFinished = false or sp.usedSessions >= sp.totalSessions)
            order by sp.assignedDate desc, sp.id desc
            """)
    List<StudentPackage> search(@Param("term") String term,
                                @Param("onlyRemaining") boolean onlyRemaining,
                                @Param("onlyFinished") boolean onlyFinished);
}
