package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByActiveTrueOrderByLastNameAscFirstNameAsc();

    /** Ad, soyad veya telefona göre aktif öğrenci arar. */
    @Query("""
            select s from Student s
            where s.active = true
              and (lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :term, '%'))
                   or s.phone like concat('%', :term, '%'))
            order by s.lastName, s.firstName
            """)
    List<Student> searchActive(@Param("term") String term);

    boolean existsByPhoneAndActiveTrue(String phone);

    boolean existsByPhoneAndActiveTrueAndIdNot(String phone, Long id);
}
