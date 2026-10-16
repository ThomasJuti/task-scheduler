package com.jutinico.scheduler.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jutinico.scheduler.config.AppSecurityProperties;
import org.springframework.stereotype.Service;

/**
 * Protección contra fuerza bruta: tras N logins fallidos del mismo usuario + IP dentro de la duración del bloqueo,
 * el par queda bloqueado esa misma duración. En memoria, suficiente para una sola instancia.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    public LoginAttemptService(AppSecurityProperties properties, Clock clock) {
        this.maxAttempts = properties.maxLoginAttempts();
        this.lockDuration = properties.loginLockDuration();
        this.clock = clock;
    }

    public Duration lockRemaining(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.failures() < maxAttempts) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), expiry(current));
        if (remaining.isNegative() || remaining.isZero()) {
            attempts.remove(key, current);
            return Duration.ZERO;
        }
        return remaining;
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        attempts.values().removeIf(entry -> isExpired(entry, now));
        attempts.compute(key, (k, current) -> {
            int previous = current == null || isExpired(current, now) ? 0 : current.failures();
            return new Attempts(previous + 1, now);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    /**
     * Mientras está bloqueado no se registran fallos (el login se rechaza antes de revisar la contraseña), así que el
     * bloqueo termina {@code lockDuration} después del último fallo.
     */
    private Instant expiry(Attempts entry) {
        return entry.lastFailure().plus(lockDuration);
    }

    private boolean isExpired(Attempts entry, Instant now) {
        return !now.isBefore(expiry(entry));
    }
}
