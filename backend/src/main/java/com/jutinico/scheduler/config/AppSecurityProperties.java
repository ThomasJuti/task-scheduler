package com.jutinico.scheduler.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Security settings, all overridable through environment variables (see application.yml and .env.example). */
@ConfigurationProperties("app.security")
public record AppSecurityProperties(
        String jwtSecret,
        Duration jwtTtl,
        String adminUsername,
        String adminPassword,
        String userUsername,
        String userPassword,
        List<String> corsAllowedOrigins,
        int maxLoginAttempts,
        Duration loginLockDuration) {
}
