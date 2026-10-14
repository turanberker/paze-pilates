package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Studio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioRepository extends JpaRepository<Studio, Long> {

    /** Tek şube kullanıldığı için stüdyo kaydı tektir. */
    Optional<Studio> findFirstByOrderByIdAsc();
}
