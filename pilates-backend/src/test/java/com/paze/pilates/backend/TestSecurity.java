package com.paze.pilates.backend;

import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.FixedRolePermissionResolver;
import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.PasswordPolicyProperties;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.backend.security.SessionContext;
import com.paze.pilates.dto.UserRole;

/** Servis testlerinde gerçek yetki tablosuyla çalışan guard üretir. */
public final class TestSecurity {

    private TestSecurity() {
    }

    /** Varsayılan kural: yalnızca en az 8 karakter. */
    public static PasswordPolicy defaultPasswordPolicy() {
        return new PasswordPolicy(new PasswordPolicyProperties(null, false, false, false, false));
    }

    public static PermissionGuard guardFor(CurrentUser user) {
        SessionContext session = new SessionContext();
        if (user != null) {
            session.set(user);
        }
        return new PermissionGuard(session, new FixedRolePermissionResolver());
    }

    public static CurrentUser admin() {
        return new CurrentUser(1L, "admin", "Yönetici", UserRole.ADMIN, null, false, false);
    }

    public static CurrentUser instructor(long userId, long instructorId) {
        return new CurrentUser(userId, "hoca" + userId, "Hoca", UserRole.INSTRUCTOR, instructorId, false, false);
    }

    public static PermissionGuard adminGuard() {
        return guardFor(admin());
    }
}
