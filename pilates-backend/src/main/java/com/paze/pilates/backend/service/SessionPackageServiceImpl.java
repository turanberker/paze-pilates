package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SessionPackageDto;
import com.paze.pilates.service.SessionPackageService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionPackageServiceImpl implements SessionPackageService {

    private final SessionPackageRepository repository;

    public SessionPackageServiceImpl(SessionPackageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionPackageDto> findAll() {
        return repository.findAllByOrderByStartDateDescNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionPackageDto> findActive() {
        return repository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SessionPackageDto save(SessionPackageDto dto) {
        validate(dto);

        SessionPackage p;
        if (dto.id() == null) {
            p = new SessionPackage(dto.name().trim(), scale(dto.price()),
                    dto.sessionCount(), dto.startDate());
        } else {
            p = repository.findById(dto.id())
                    .orElseThrow(() -> new BusinessException("package.error.not_found"));
            p.setName(dto.name().trim());
            p.setPrice(scale(dto.price()));
            p.setSessionCount(dto.sessionCount());
            p.setStartDate(dto.startDate());
        }
        p.setActive(dto.active());
        return toDto(repository.save(p));
    }

    private void validate(SessionPackageDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new BusinessException("package.error.name_required");
        }
        if (dto.name().trim().length() > 100) {
            throw new BusinessException("package.error.name_too_long");
        }
        if (dto.price() == null || dto.price().signum() < 0) {
            throw new BusinessException("package.error.price_invalid");
        }
        if (dto.price().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessException("package.error.price_invalid");
        }
        if (dto.sessionCount() == null || dto.sessionCount() < 1) {
            throw new BusinessException("package.error.session_count_invalid");
        }
        if (dto.startDate() == null) {
            throw new BusinessException("package.error.start_date_required");
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private SessionPackageDto toDto(SessionPackage p) {
        return new SessionPackageDto(p.getId(), p.getName(), p.getPrice(),
                p.getSessionCount(), p.getStartDate(), p.isActive());
    }
}
