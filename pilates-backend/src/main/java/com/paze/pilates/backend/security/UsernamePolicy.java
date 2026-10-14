package com.paze.pilates.backend.security;

import com.paze.pilates.dto.BusinessException;

public final class UsernamePolicy {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    private UsernamePolicy() {
    }

    /** Kullanıcı adını kırpıp doğrular; sabit adminin adı kullanılamaz. */
    public static String normalize(String raw, StaticAdminProperties staticAdmin) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("user.error.username_required");
        }
        String username = raw.trim();
        if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
            throw new BusinessException("user.error.username_length");
        }
        if (username.chars().anyMatch(Character::isWhitespace)) {
            throw new BusinessException("user.error.username_invalid");
        }
        if (staticAdmin.isStaticUsername(username)) {
            throw new BusinessException("user.error.username_reserved");
        }
        return username;
    }
}
