package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.backend.security.UsernamePolicy;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserDto;
import com.paze.pilates.dto.UserRole;
import com.paze.pilates.service.UserService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final int FULL_NAME_MAX_LENGTH = 100;

    private final AppUserRepository userRepository;
    private final InstructorRepository instructorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionGuard guard;
    private final StaticAdminProperties staticAdmin;

    public UserServiceImpl(AppUserRepository userRepository,
                           InstructorRepository instructorRepository,
                           PasswordEncoder passwordEncoder,
                           PermissionGuard guard,
                           StaticAdminProperties staticAdmin) {
        this.userRepository = userRepository;
        this.instructorRepository = instructorRepository;
        this.passwordEncoder = passwordEncoder;
        this.guard = guard;
        this.staticAdmin = staticAdmin;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        guard.require(Permission.USER_MANAGE);
        return userRepository.findAllByOrderByUsernameAsc().stream().map(UserServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public UserDto createUser(String username, String fullName, UserRole role, Long instructorId,
                              String temporaryPassword) {
        guard.require(Permission.USER_MANAGE);
        if (role == null) {
            throw new BusinessException("user.error.role_required");
        }
        String normalized = UsernamePolicy.normalize(username, staticAdmin);
        if (userRepository.existsByUsername(normalized)) {
            throw new BusinessException("user.error.username_taken");
        }
        PasswordPolicy.validate(temporaryPassword, null);

        Instructor instructor = null;
        String name;
        if (role == UserRole.INSTRUCTOR) {
            instructor = requireFreeInstructor(instructorId, null);
            name = instructor.getFirstName() + " " + instructor.getLastName();
        } else {
            if (instructorId != null) {
                throw new BusinessException("user.error.instructor_only_for_instructor_role");
            }
            if (fullName == null || fullName.isBlank()) {
                throw new BusinessException("user.error.full_name_required");
            }
            name = fullName.trim();
            if (name.length() > FULL_NAME_MAX_LENGTH) {
                throw new BusinessException("user.error.full_name_too_long");
            }
        }

        AppUser user = new AppUser(normalized, passwordEncoder.encode(temporaryPassword), name, role);
        user.setInstructor(instructor);
        user.setMustChangePassword(true);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto resetPassword(Long userId, String temporaryPassword) {
        guard.require(Permission.USER_MANAGE);
        AppUser user = find(userId);
        PasswordPolicy.validate(temporaryPassword, null);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto activate(Long userId) {
        guard.require(Permission.USER_MANAGE);
        AppUser user = find(userId);
        if (user.getRole() == UserRole.INSTRUCTOR
                && (user.getInstructor() == null || !user.getInstructor().isActive())) {
            throw new BusinessException("user.error.instructor_not_active");
        }
        user.setActive(true);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto deactivate(Long userId) {
        CurrentUser current = guard.require(Permission.USER_MANAGE);
        AppUser user = find(userId);
        if (user.getId().equals(current.id())) {
            throw new BusinessException("user.error.self_deactivate");
        }
        ensureNotLastActiveAdmin(user);
        user.setActive(false);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto changeRole(Long userId, UserRole role, Long instructorId) {
        guard.require(Permission.USER_MANAGE);
        if (role == null) {
            throw new BusinessException("user.error.role_required");
        }
        AppUser user = find(userId);
        if (role == UserRole.INSTRUCTOR) {
            user.setInstructor(requireFreeInstructor(instructorId, user.getId()));
        } else {
            if (instructorId != null) {
                throw new BusinessException("user.error.instructor_only_for_instructor_role");
            }
            user.setInstructor(null);
        }
        if (role != UserRole.ADMIN) {
            ensureNotLastActiveAdmin(user);
        }
        user.setRole(role);
        return toDto(userRepository.save(user));
    }

    /** Aktif son yönetici pasife alınamaz veya rolü düşürülemez; sabit admin bu sayıma girmez. */
    private void ensureNotLastActiveAdmin(AppUser user) {
        if (user.isActive() && user.getRole() == UserRole.ADMIN
                && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new BusinessException("user.error.last_admin");
        }
    }

    private Instructor requireFreeInstructor(Long instructorId, Long ownUserId) {
        if (instructorId == null) {
            throw new BusinessException("user.error.instructor_required");
        }
        Instructor instructor = instructorRepository.findById(instructorId)
                .filter(Instructor::isActive)
                .orElseThrow(() -> new BusinessException("user.error.instructor_not_active"));
        boolean taken = ownUserId == null
                ? userRepository.existsByInstructorId(instructorId)
                : userRepository.existsByInstructorIdAndIdNot(instructorId, ownUserId);
        if (taken) {
            throw new BusinessException("user.error.instructor_already_linked");
        }
        return instructor;
    }

    private AppUser find(Long userId) {
        return userRepository.findById(userId == null ? -1L : userId)
                .orElseThrow(() -> new BusinessException("user.error.not_found"));
    }

    private static UserDto toDto(AppUser user) {
        Instructor instructor = user.getInstructor();
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                instructor == null ? null : instructor.getId(),
                instructor == null ? null : instructor.getFirstName() + " " + instructor.getLastName(),
                user.isActive(), user.isMustChangePassword());
    }
}
