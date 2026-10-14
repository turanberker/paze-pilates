package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Instructor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    List<Instructor> findByActiveTrueOrderByFirstNameAscLastNameAsc();

    @EntityGraph(attributePaths = "availabilities")
    @Query("select i from Instructor i where i.id = :id")
    Optional<Instructor> findWithAvailabilitiesById(@Param("id") Long id);
}
