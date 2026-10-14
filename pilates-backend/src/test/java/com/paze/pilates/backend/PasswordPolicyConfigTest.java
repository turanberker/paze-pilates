package com.paze.pilates.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.security.PasswordPolicy;
import com.paze.pilates.backend.security.PasswordPolicyProperties;
import com.paze.pilates.dto.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Şifre kurallarının yml/özellik üzerinden bağlandığını doğrular. */
@SpringBootTest(properties = {
        "paze.security.password.min-length=10",
        "paze.security.password.require-digit=true"
})
class PasswordPolicyConfigTest {

    @Autowired
    private PasswordPolicyProperties properties;
    @Autowired
    private PasswordPolicy policy;

    @Test
    void propertiesAreBoundAndEnforced() {
        assertThat(properties.minLength()).isEqualTo(10);
        assertThat(properties.requireDigit()).isTrue();
        assertThat(properties.requireUppercase()).isFalse();

        assertThatThrownBy(() -> policy.validate("kisa1", null)).isInstanceOf(BusinessException.class)
                .hasMessage("password.error.too_short");
        assertThatThrownBy(() -> policy.validate("rakamyokturr", null)).hasMessage("password.error.digit_required");
        policy.validate("rakamvar1234", "rakamvar1234");
    }
}
