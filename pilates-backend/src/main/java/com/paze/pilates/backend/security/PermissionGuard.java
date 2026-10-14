package com.paze.pilates.backend.security;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import org.springframework.stereotype.Component;

/** Servis metotlarının başında çağrılır; ekranın menü gizlemesi güvenlik sayılmaz. */
@Component
public class PermissionGuard {

    private final SessionContext session;
    private final RolePermissionResolver resolver;

    public PermissionGuard(SessionContext session, RolePermissionResolver resolver) {
        this.session = session;
        this.resolver = resolver;
    }

    /** Giriş yapılmış ve geçici şifresini değiştirmiş kullanıcıyı döner. */
    public CurrentUser requireLoggedIn() {
        CurrentUser user = session.get().orElseThrow(() -> new BusinessException("auth.error.not_authenticated"));
        if (user.mustChangePassword()) {
            throw new BusinessException("auth.error.password_change_required");
        }
        return user;
    }

    public CurrentUser require(Permission permission) {
        return requireAny(permission);
    }

    public CurrentUser requireAny(Permission... permissions) {
        CurrentUser user = requireLoggedIn();
        for (Permission permission : permissions) {
            if (resolver.permissionsOf(user.role()).contains(permission)) {
                return user;
            }
        }
        throw new BusinessException("auth.error.forbidden");
    }

    public boolean has(Permission permission) {
        return session.get()
                .map(user -> resolver.permissionsOf(user.role()).contains(permission))
                .orElse(false);
    }
}
