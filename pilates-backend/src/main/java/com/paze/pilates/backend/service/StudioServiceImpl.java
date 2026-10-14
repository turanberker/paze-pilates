package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.Studio;
import com.paze.pilates.backend.mapper.RoomMapper;
import com.paze.pilates.backend.mapper.StudioMapper;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.backend.repository.StudioRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.dto.StudioDto;
import com.paze.pilates.service.StudioService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioServiceImpl implements StudioService {

    private final StudioRepository studioRepository;
    private final RoomRepository roomRepository;
    private final PermissionGuard guard;
    private final StudioMapper studioMapper;
    private final RoomMapper roomMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<StudioDto> getStudio() {
        guard.require(Permission.STUDIO_MANAGE);
        return studioRepository.findFirstByOrderByIdAsc()
                .map(studioMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> getLogo() {
        guard.requireLoggedIn();
        return studioRepository.findFirstByOrderByIdAsc()
                .map(Studio::getLogo);
    }

    @Override
    @Transactional
    public StudioDto saveStudio(StudioDto dto) {
        guard.require(Permission.STUDIO_MANAGE);
        if (dto.name() == null || dto.name().isBlank()) {
            throw new BusinessException("studio.error.name_required");
        }
        Studio studio = (dto.id() != null)
                ? studioRepository.findById(dto.id()).orElseThrow()
                : new Studio(dto.name());
        studio.setName(dto.name().trim());
        Studio saved = studioRepository.save(studio);
        return studioMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> getRooms() {
        guard.require(Permission.STUDIO_MANAGE);
        return roomRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomDto saveRoom(RoomDto dto) {
        guard.require(Permission.STUDIO_MANAGE);
        if (dto.name() == null || dto.name().isBlank()) {
            throw new BusinessException("room.error.name_required");
        }
        if (dto.capacity() <= 0) {
            throw new BusinessException("room.error.capacity_invalid");
        }
        if (dto.id() == null) {
            if (roomRepository.existsByNameIgnoreCase(dto.name().trim())) {
                throw new BusinessException("room.error.name_duplicate");
            }
            Room room = new Room(dto.name().trim(), dto.capacity());
            Room saved = roomRepository.save(room);
            return roomMapper.toDto(saved);
        } else {
            Room room = roomRepository.findById(dto.id()).orElseThrow();
            if (!room.getName().equalsIgnoreCase(dto.name().trim())
                    && roomRepository.existsByNameIgnoreCase(dto.name().trim())) {
                throw new BusinessException("room.error.name_duplicate");
            }
            room.setName(dto.name().trim());
            room.setCapacity(dto.capacity());
            Room saved = roomRepository.save(room);
            return roomMapper.toDto(saved);
        }
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        guard.require(Permission.STUDIO_MANAGE);
        roomRepository.findById(id).ifPresent(room -> {
            room.setActive(false);
            roomRepository.save(room);
        });
    }
}
