package com.paze.pilates.backend.security;

import com.paze.pilates.dto.BusinessException;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** bcrypt yalnızca ilk 72 baytı kullanır. */
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** @param confirm null ise tekrar kontrolü yapılmaz */
    public static void validate(String password, String confirm) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("password.error.required");
        }
        if (password.length() < MIN_LENGTH) {
            throw new BusinessException("password.error.too_short");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BusinessException("password.error.too_long");
        }
        if (confirm != null && !password.equals(confirm)) {
            throw new BusinessException("password.error.mismatch");
        }
    }
}
