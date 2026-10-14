package com.paze.pilates.backend.config;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.UserRole;
import com.paze.pilates.backend.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İlk çalışmada veritabanında hiç kullanıcı yoksa varsayılan admin hesabı oluşturur.
 * Varsayılan giriş: admin / admin123
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            AppUser admin = new AppUser(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Yönetici",
                    UserRole.ADMIN
            );
            userRepository.save(admin);
            log.info("Varsayilan admin hesabi olusturuldu. Kullanici: admin / Sifre: admin123");
        }
    }
}
