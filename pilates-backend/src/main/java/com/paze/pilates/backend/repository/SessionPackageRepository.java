package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.SessionPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionPackageRepository extends JpaRepository<SessionPackage, Long> {

    List<SessionPackage> findAllByOrderByStartDateDescNameAsc();

    List<SessionPackage> findByActiveTrueOrderByNameAsc();
}
