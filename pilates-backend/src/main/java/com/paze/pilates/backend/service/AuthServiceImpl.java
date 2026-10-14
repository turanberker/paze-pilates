package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.service.AuthService;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private AppUser currentUser;

    public AuthServiceImpl(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(AppUser::isActive)
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPasswordHash())) {
                        this.currentUser = user;
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public Optional<AppUserDto> getCurrentUser() {
        if (currentUser == null) {
            return Optional.empty();
        }
        return Optional.of(new AppUserDto(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getFullName(),
                currentUser.getRole().name()
        ));
    }

    @Override
    public void logout() {
        this.currentUser = null;
    }
}
