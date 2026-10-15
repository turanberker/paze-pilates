package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.StudentPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, Long> {

    /**
     * Seans düşülebilecek paketler sırasıyla: önce ödemesi alınmış, sonra alınmamış; her grupta en eski atama önce.
     */
    @Query("""
            select sp from StudentPackage sp
            where sp.student.id = :studentId and sp.usedSessions < sp.totalSessions
            order by sp.paid desc, sp.assignedDate, sp.id
            """)
    List<StudentPackage> findUsableForStudent(@Param("studentId") Long studentId);

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

    /** Verilen gün ödemesi alınan paketlerin fiyat toplamı. */
    @Query("""
            select coalesce(sum(p.price), 0) from StudentPackage sp join sp.sessionPackage p
            where sp.paid = true and sp.paymentDate = :date
            """)
    BigDecimal sumPriceCollectedOn(@Param("date") LocalDate date);

    /** Verilen gün ödemesi alınan paket sayısı. */
    @Query("select count(sp) from StudentPackage sp where sp.paid = true and sp.paymentDate = :date")
    long countCollectedOn(@Param("date") LocalDate date);

    /** Verilen tarih aralığında (uçlar dahil) ödemesi alınan paketlerin fiyat toplamı. */
    @Query("""
            select coalesce(sum(p.price), 0) from StudentPackage sp join sp.sessionPackage p
            where sp.paid = true and sp.paymentDate between :from and :to
            """)
    BigDecimal sumPriceCollectedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Verilen tarih aralığında (uçlar dahil) ödemesi alınan paket sayısı. */
    @Query("select count(sp) from StudentPackage sp where sp.paid = true and sp.paymentDate between :from and :to")
    long countCollectedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Ödemesi alınmamış paketlerin fiyat toplamı. */
    @Query("""
            select coalesce(sum(p.price), 0) from StudentPackage sp join sp.sessionPackage p
            where sp.paid = false
            """)
    BigDecimal sumPriceUnpaid();

    /** Ödemesi alınmamış paket sayısı. */
    @Query("select count(sp) from StudentPackage sp where sp.paid = false")
    long countUnpaid();

    /** Seansı bitmemiş ve kalanı en fazla {@code maxRemaining} olan paket sayısı. */
    @Query("""
            select count(sp) from StudentPackage sp
            where sp.usedSessions < sp.totalSessions
              and sp.totalSessions - sp.usedSessions <= :maxRemaining
            """)
    long countExpiring(@Param("maxRemaining") int maxRemaining);
}
