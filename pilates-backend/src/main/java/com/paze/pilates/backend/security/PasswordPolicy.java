package com.paze.pilates.backend.security;

import com.paze.pilates.dto.BusinessException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Uygulamada belirlenen şifreler için kurallar; sabit admin şifresi (yalnızca hash) buradan geçmez. */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    /** bcrypt yalnızca ilk 72 baytı kullanır. */
    public static final int MAX_BYTES = 72;

    private final PasswordPolicyProperties properties;

    /** @param confirm null ise tekrar kontrolü yapılmaz */
    public void validate(String password, String confirm) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("password.error.required");
        }
        if (password.length() < properties.minLength()) {
            throw new BusinessException("password.error.too_short", properties.minLength());
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BusinessException("password.error.too_long");
        }
        if (properties.requireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new BusinessException("password.error.uppercase_required");
        }
        if (properties.requireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new BusinessException("password.error.lowercase_required");
        }
        if (properties.requireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessException("password.error.digit_required");
        }
        if (properties.requireSpecial() && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new BusinessException("password.error.special_required");
        }
        if (confirm != null && !password.equals(confirm)) {
            throw new BusinessException("password.error.mismatch");
        }
    }
}
