package com.paze.pilates.backend.security;

import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserRole;
import java.util.Set;

/**
 * Rol → yetki eşlemesi. Çoklu kiracı geldiğinde kiracıya göre izin veren yeni bir uygulama
 * yazılır (imzaya kiracı parametresi eklenir); çağıranlar yalnızca {@link PermissionGuard}'dır.
 */
public interface RolePermissionResolver {

    Set<Permission> permissionsOf(UserRole role);
}
