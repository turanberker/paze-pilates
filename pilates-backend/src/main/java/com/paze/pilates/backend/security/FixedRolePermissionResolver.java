package com.paze.pilates.backend.security;

import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserRole;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Sabit rol tablosu: yönetici her şey; hoca tüm dersleri görür, yalnızca kendi derslerini yönetir ve öğrenci tanımlayabilir. */
@Component
public class FixedRolePermissionResolver implements RolePermissionResolver {

    private static final Set<Permission> ADMIN = EnumSet.allOf(Permission.class);
    private static final Set<Permission> INSTRUCTOR = EnumSet.of(Permission.LESSON_VIEW_ALL, Permission.LESSON_MANAGE_OWN,
            Permission.STUDENT_MANAGE);

    @Override
    public Set<Permission> permissionsOf(UserRole role) {
        return switch (role) {
            case ADMIN -> ADMIN;
            case INSTRUCTOR -> INSTRUCTOR;
        };
    }
}
