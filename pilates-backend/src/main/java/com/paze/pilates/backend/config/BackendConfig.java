package com.paze.pilates.backend.config;

import com.paze.pilates.backend.security.PasswordPolicyProperties;
import com.paze.pilates.backend.security.StaticAdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({StaticAdminProperties.class, PasswordPolicyProperties.class})
public class BackendConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
