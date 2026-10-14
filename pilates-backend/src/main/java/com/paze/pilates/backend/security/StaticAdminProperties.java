package com.paze.pilates.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Yapılandırmadan gelen sabit admin. Veritabanında durmaz; {@code passwordHash} bcrypt hash'idir,
 * boşsa sabit admin kapalıdır. Düz şifre hiçbir yere yazılmaz.
 */
@ConfigurationProperties(prefix = "paze.security.static-admin")
public record StaticAdminProperties(String username, String passwordHash) {

    public boolean isEnabled() {
        return username != null && !username.isBlank() && passwordHash != null && !passwordHash.isBlank();
    }

    public boolean isStaticUsername(String candidate) {
        return username != null && !username.isBlank() && username.equals(candidate);
    }
}
