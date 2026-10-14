package com.paze.pilates.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class BackendContextTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoadsAndPasswordEncoderHashes() {
        String hash = passwordEncoder.encode("secret");

        assertThat(hash).isNotEqualTo("secret");
        assertThat(passwordEncoder.matches("secret", hash)).isTrue();
    }
}
