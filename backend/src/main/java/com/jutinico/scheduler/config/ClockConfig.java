package com.jutinico.scheduler.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code app.timezone} is the single source of "today" for the services and for Bean Validation. */
@Configuration
public class ClockConfig {

    /** Injected wherever "today" matters so tests can pin the date. */
    @Bean
    Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    /**
     * Makes {@code @FutureOrPresent} and friends use the same clock; by default they use the JVM time zone,
     * which could accept or reject "today" differently from the services.
     */
    @Bean
    ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
