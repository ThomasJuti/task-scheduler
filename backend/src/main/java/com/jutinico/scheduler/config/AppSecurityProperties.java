package com.jutinico.scheduler.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
