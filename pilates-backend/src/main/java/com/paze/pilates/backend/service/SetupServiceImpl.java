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
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetupServiceImpl implements SetupService {

    private static final int NAME_MAX_LENGTH = 100;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final AppUserRepository userRepository;
    private final StudioRepository studioRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaticAdminProperties staticAdmin;
    private final PasswordPolicy passwordPolicy;

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
        passwordPolicy.validate(request.password(), request.passwordConfirm());
        byte[] logo = validLogo(request.logo());

        studioRepository.findFirstByOrderByIdAsc().ifPresentOrElse(
                studio -> {
                    studio.setName(studioName);
                    studio.setLogo(logo);
                    studioRepository.save(studio);
                },
                () -> {
                    Studio studio = new Studio(studioName);
                    studio.setLogo(logo);
                    studioRepository.save(studio);
                });

        userRepository.save(new AppUser(username, passwordEncoder.encode(request.password()),
                fullName, UserRole.ADMIN));
    }

    /** Logo isteğe bağlıdır; verildiyse boyutu sınırlıdır ve PNG ya da JPEG olmalıdır. */
    private static byte[] validLogo(byte[] logo) {
        if (logo == null || logo.length == 0) {
            return null;
        }
        if (logo.length > SetupRequestDto.LOGO_MAX_BYTES) {
            throw new BusinessException("setup.error.logo_too_large", SetupRequestDto.LOGO_MAX_BYTES / 1024);
        }
        if (!startsWith(logo, PNG_SIGNATURE) && !startsWith(logo, JPEG_SIGNATURE)) {
            throw new BusinessException("setup.error.logo_invalid_format");
        }
        return logo;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length
                && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
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
