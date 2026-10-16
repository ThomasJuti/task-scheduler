package com.jutinico.scheduler.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jutinico.scheduler.config.AppSecurityProperties;
import org.springframework.stereotype.Service;

/**
 * Basic brute-force protection: after N failed logins for the same username + IP the pair is locked for a
 * while. In memory, which is enough for a single instance.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {
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

    /** Remaining lock time, or {@link Duration#ZERO} when the key may try again. */
    public Duration lockRemaining(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), current.lockedUntil());
        if (remaining.isNegative() || remaining.isZero()) {
            attempts.remove(key);
            return Duration.ZERO;
        }
        return remaining;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, current) -> {
            int failures = (current == null ? 0 : current.failures()) + 1;
            Instant lockedUntil = failures >= maxAttempts ? clock.instant().plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
