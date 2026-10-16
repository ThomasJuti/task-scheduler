package com.jutinico.scheduler.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jutinico.scheduler.config.AppSecurityProperties;
import org.springframework.stereotype.Service;

/**
 * Basic brute-force protection: after N failed logins for the same username + IP within the lock duration, the
 * pair is locked for that same duration. Failures older than that are forgotten, so occasional typos never add
 * up to a lock and the map does not grow forever. In memory, which is enough for a single instance.
 */
@Service
public class LoginAttemptService {

    /** Failures counted so far and when the last one happened (also when the lock, if any, started). */
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

    /** Remaining lock time, or {@link Duration#ZERO} when the key may try again. */
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
     * A failure counts for {@code lockDuration}. While locked no new failures are recorded (the login is
     * rejected before the password is checked), so the lock also ends {@code lockDuration} after the last one.
     */
    private Instant expiry(Attempts entry) {
        return entry.lastFailure().plus(lockDuration);
    }

    private boolean isExpired(Attempts entry, Instant now) {
        return !now.isBefore(expiry(entry));
    }
}
