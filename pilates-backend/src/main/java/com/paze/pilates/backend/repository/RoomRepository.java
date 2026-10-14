package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
