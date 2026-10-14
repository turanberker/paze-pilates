package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.RolePermissionResolver;
import com.paze.pilates.backend.security.SessionContext;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.UserRole;
import com.paze.pilates.service.AuthService;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionContext session;
    private final RolePermissionResolver permissionResolver;
    private final StaticAdminProperties staticAdmin;
    private final PasswordPolicy passwordPolicy;

    public AuthServiceImpl(AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionContext session,
                           RolePermissionResolver permissionResolver,
                           StaticAdminProperties staticAdmin,
                           PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.session = session;
        this.permissionResolver = permissionResolver;
        this.staticAdmin = staticAdmin;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        session.clear();
        if (username == null || password == null || username.isBlank() || password.isEmpty()) {
            return false;
        }
        if (staticAdmin.isEnabled() && staticAdmin.isStaticUsername(username)) {
            // Sabit admin açıkken adı veritabanına düşmez; kapalıysa (hash boş) normal kullanıcı gibi aranır.
            if (!passwordEncoder.matches(password, staticAdmin.passwordHash())) {
                return false;
            }
            session.set(new CurrentUser(null, username, username, UserRole.ADMIN, null, true, false));
            return true;
        }
        return userRepository.findByUsername(username)
                .filter(AppUser::isActive)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> {
                    session.set(new CurrentUser(user.getId(), user.getUsername(), user.getFullName(),
                            user.getRole(),
                            user.getInstructor() == null ? null : user.getInstructor().getId(),
                            false, user.isMustChangePassword()));
                    return true;
                })
                .orElse(false);
    }

    @Override
    public Optional<AppUserDto> getCurrentUser() {
        return session.get().map(user -> new AppUserDto(user.id(), user.username(), user.fullName(),
                user.role(), user.instructorId(), user.mustChangePassword(), user.staticAdmin(),
                permissionResolver.permissionsOf(user.role())));
    }

    @Override
    public void logout() {
        session.clear();
    }

    @Override
    @Transactional
    public void changeOwnPassword(String currentPassword, String newPassword, String newPasswordConfirm) {
        CurrentUser current = session.get()
                .orElseThrow(() -> new BusinessException("auth.error.not_authenticated"));
        if (current.staticAdmin()) {
            throw new BusinessException("auth.error.static_admin_password");
        }
        AppUser user = userRepository.findById(current.id())
                .orElseThrow(() -> new BusinessException("auth.error.not_authenticated"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("auth.error.current_password_wrong");
        }
        passwordPolicy.validate(newPassword, newPasswordConfirm);
        if (newPassword.equals(currentPassword)) {
            throw new BusinessException("password.error.same_as_current");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        session.set(current.withPasswordChanged());
    }
}
