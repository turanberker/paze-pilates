package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.PasswordPolicyProperties;
import com.paze.pilates.dto.BusinessException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private static PasswordPolicy policy(Integer min, boolean upper, boolean lower, boolean digit, boolean special) {
        return new PasswordPolicy(new PasswordPolicyProperties(min, upper, lower, digit, special));
    }

    @Test
    void defaultsRequireOnlyEightCharacters() {
        PasswordPolicy defaults = policy(null, false, false, false, false);

        defaults.validate("sekizkrk", "sekizkrk");
        assertThatThrownBy(() -> defaults.validate("kisa", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("password.error.too_short")
                .satisfies(e -> assertThat(((BusinessException) e).getArgs()).containsExactly(8));
    }

    @Test
    void configuredMinLengthIsUsed() {
        PasswordPolicy twelve = policy(12, false, false, false, false);

        assertThatThrownBy(() -> twelve.validate("onbirkarakt", null))
                .satisfies(e -> assertThat(((BusinessException) e).getArgs()).containsExactly(12));
        twelve.validate("oniki-karakter", null);
    }

    @Test
    void requiredCharacterClassesAreEnforced() {
        PasswordPolicy strict = policy(8, true, true, true, true);

        assertThatThrownBy(() -> strict.validate("kucuk123!", null)).hasMessage("password.error.uppercase_required");
        assertThatThrownBy(() -> strict.validate("BUYUK123!", null)).hasMessage("password.error.lowercase_required");
        assertThatThrownBy(() -> strict.validate("Harfler!!", null)).hasMessage("password.error.digit_required");
        assertThatThrownBy(() -> strict.validate("Harf1234", null)).hasMessage("password.error.special_required");
        strict.validate("Harf123!", "Harf123!");
    }

    @Test
    void blankTooLongAndMismatchAreRejected() {
        PasswordPolicy defaults = policy(null, false, false, false, false);

        assertThatThrownBy(() -> defaults.validate("", null)).hasMessage("password.error.required");
        assertThatThrownBy(() -> defaults.validate("a".repeat(73), null)).hasMessage("password.error.too_long");
        assertThatThrownBy(() -> defaults.validate("sifre1234", "sifre12345")).hasMessage("password.error.mismatch");
    }
}
