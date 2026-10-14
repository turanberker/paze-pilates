package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.dto.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "instructor")
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = "instructor")
    List<AppUser> findAllByOrderByUsernameAsc();

    Optional<AppUser> findByInstructorId(Long instructorId);

    boolean existsByInstructorId(Long instructorId);

    boolean existsByInstructorIdAndIdNot(Long instructorId, Long id);

    long countByRoleAndActiveTrue(UserRole role);
}
