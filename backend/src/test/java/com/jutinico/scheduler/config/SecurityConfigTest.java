package com.jutinico.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void rejectsTheExampleSecretFromEnvExample() {
        assertThatThrownBy(() -> config.jwtSecretKey(withSecret("change-me-to-a-long-random-secret-of-at-least-32-chars")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example value");
    }

    @Test
    void rejectsShortSecrets() {
        assertThatThrownBy(() -> config.jwtSecretKey(withSecret("too-short")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void acceptsARandomSecret() {
        assertThat(config.jwtSecretKey(withSecret("q8Zr2LkVw9XyT4nB7mC1pD6sF3hJ5gKa0eWuYiOo"))).isNotNull();
    }

    private static AppSecurityProperties withSecret(String secret) {
        return new AppSecurityProperties(secret, Duration.ofHours(1), "admin", "a", "user", "u",
                List.of("http://localhost:4200"), 5, Duration.ofMinutes(5));
    }
}
