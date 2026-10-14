package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Studio;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.StudioRepository;
import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.backend.security.UsernamePolicy;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SetupRequestDto;
import com.paze.pilates.dto.UserRole;
import com.paze.pilates.service.SetupService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupServiceImpl implements SetupService {

    private static final int NAME_MAX_LENGTH = 100;

    private final AppUserRepository userRepository;
    private final StudioRepository studioRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaticAdminProperties staticAdmin;

    public SetupServiceImpl(AppUserRepository userRepository,
                            StudioRepository studioRepository,
                            PasswordEncoder passwordEncoder,
                            StaticAdminProperties staticAdmin) {
        this.userRepository = userRepository;
        this.studioRepository = studioRepository;
        this.passwordEncoder = passwordEncoder;
        this.staticAdmin = staticAdmin;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    @Override
    public boolean isStaticAdminAvailable() {
        return staticAdmin.isEnabled();
    }

    @Override
    @Transactional
    public void completeSetup(SetupRequestDto request) {
        if (userRepository.count() > 0) {
            throw new BusinessException("setup.error.already_done");
        }
        String studioName = required(request.studioName(), "setup.error.studio_name_required",
                "setup.error.studio_name_too_long");
        String fullName = required(request.adminFullName(), "setup.error.full_name_required",
                "setup.error.full_name_too_long");
        String username = UsernamePolicy.normalize(request.username(), staticAdmin);
        PasswordPolicy.validate(request.password(), request.passwordConfirm());

        studioRepository.findFirstByOrderByIdAsc().ifPresentOrElse(
                studio -> {
                    studio.setName(studioName);
                    studioRepository.save(studio);
                },
                () -> studioRepository.save(new Studio(studioName)));

        userRepository.save(new AppUser(username, passwordEncoder.encode(request.password()),
                fullName, UserRole.ADMIN));
    }

    private static String required(String value, String requiredKey, String tooLongKey) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(requiredKey);
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(tooLongKey);
        }
        return trimmed;
    }
}
