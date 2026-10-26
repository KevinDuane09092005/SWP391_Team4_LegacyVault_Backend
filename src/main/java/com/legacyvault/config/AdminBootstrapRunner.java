package com.legacyvault.config;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String fullName;
    private final String password;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.enabled:false}") boolean enabled,
            @Value("${admin.bootstrap.email:}") String email,
            @Value("${admin.bootstrap.full-name:LegacyVault Admin}") String fullName,
            @Value("${admin.bootstrap.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (email == null || email.isBlank() || password == null || password.length() < 16) {
            throw new IllegalStateException(
                    "Set ADMIN_BOOTSTRAP_EMAIL and a password of at least 16 characters when admin bootstrap is enabled");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.info("Admin bootstrap skipped because the configured account already exists");
            return;
        }

        User admin = new User();
        admin.setFullName(fullName == null || fullName.isBlank() ? "LegacyVault Admin" : fullName.trim());
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        admin.setIsVerified(true);
        admin.setIsActive(true);
        userRepository.save(admin);
        logger.info("Initial administrator account created. Disable admin bootstrap after this run.");
    }
}
