package com.paze.pilates.backend.security;

import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserRole;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Sabit rol tablosu: yönetici her şey, hoca yalnızca kendi dersleri. */
@Component
public class FixedRolePermissionResolver implements RolePermissionResolver {

    private static final Set<Permission> ADMIN = EnumSet.allOf(Permission.class);
    private static final Set<Permission> INSTRUCTOR = EnumSet.of(Permission.LESSON_VIEW_OWN);

    @Override
    public Set<Permission> permissionsOf(UserRole role) {
        return switch (role) {
            case ADMIN -> ADMIN;
            case INSTRUCTOR -> INSTRUCTOR;
        };
    }
}
