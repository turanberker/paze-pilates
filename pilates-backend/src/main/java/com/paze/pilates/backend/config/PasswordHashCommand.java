package com.paze.pilates.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Sabit admin şifresi için bcrypt hash üretir: {@code --hash-password=<şifre>}.
 * Hash'i yazdırıp uygulamayı kapatır; çıktı application-local.yml'e yapıştırılır.
 */
@Component
public class PasswordHashCommand implements ApplicationRunner {

    private static final String OPTION = "hash-password";

    private final PasswordEncoder passwordEncoder;

    public PasswordHashCommand(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION)) {
            return;
        }
        String password = args.getOptionValues(OPTION).stream().findFirst().orElse("");
        System.out.println(passwordEncoder.encode(password));
        System.exit(0);
    }
}
