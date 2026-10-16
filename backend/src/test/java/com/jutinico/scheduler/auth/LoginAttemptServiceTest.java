package com.jutinico.scheduler.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.jutinico.scheduler.config.AppSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final String KEY = "admin|10.0.0.1";
    private static final Duration LOCK = Duration.ofMinutes(5);

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-19T12:00:00Z"));
        AppSecurityProperties properties = new AppSecurityProperties("secret", Duration.ofHours(1), "admin", "x",
                "user", "y", List.of(), 3, LOCK);
        service = new LoginAttemptService(properties, clock);
    }

    @Test
    void locksAfterMaxFailures() {
        service.recordFailure(KEY);
        service.recordFailure(KEY);
        assertThat(service.lockRemaining(KEY)).isZero();

        service.recordFailure(KEY);

        assertThat(service.lockRemaining(KEY)).isEqualTo(LOCK);
        assertThat(service.lockRemaining("other|10.0.0.1")).isZero();
    }

    @Test
    void unlocksWhenTheLockDurationHasPassed() {
        failTimes(3);
        clock.advance(LOCK.minusSeconds(1));
        assertThat(service.lockRemaining(KEY)).isEqualTo(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));

        assertThat(service.lockRemaining(KEY)).isZero();
        service.recordFailure(KEY);
        assertThat(service.lockRemaining(KEY)).as("the count starts again after a lock").isZero();
    }

    @Test
    void oldFailuresExpireAndDoNotAddUpToALock() {
        failTimes(2);
        clock.advance(LOCK);

        service.recordFailure(KEY);
        service.recordFailure(KEY);

        assertThat(service.lockRemaining(KEY)).isZero();
    }

    @Test
    void successResetsTheCount() {
        failTimes(2);

        service.reset(KEY);
        failTimes(2);

        assertThat(service.lockRemaining(KEY)).isZero();
    }

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(KEY);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
