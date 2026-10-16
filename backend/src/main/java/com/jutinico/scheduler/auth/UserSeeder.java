package com.jutinico.scheduler.auth;

import com.jutinico.scheduler.config.AppSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates (or updates) the two demo accounts from environment variables, so no password lives in the code
 * or in the migrations. An account whose password variable is empty is not created.
 */
@Component
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppSecurityProperties properties;

    public UserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder, AppSecurityProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsert(properties.adminUsername(), properties.adminPassword(), Role.ADMIN);
        upsert(properties.userUsername(), properties.userPassword(), Role.USER);
    }

    private void upsert(String username, String password, Role role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No password configured for the {} account; it will not be available", role);
            return;
        }
        users.findByUsername(username).ifPresentOrElse(user -> {
            if (!passwordEncoder.matches(password, user.getPasswordHash()) || user.getRole() != role) {
                user.changeCredentials(passwordEncoder.encode(password), role);
                log.info("Updated {} account '{}'", role, username);
            }
        }, () -> {
            users.save(new AppUser(username, passwordEncoder.encode(password), role));
            log.info("Created {} account '{}'", role, username);
        });
    }
}
