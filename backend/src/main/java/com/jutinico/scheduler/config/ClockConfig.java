package com.jutinico.scheduler.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code app.timezone} es la única fuente de "hoy" para los servicios y para Bean Validation. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    /** Sin esto, {@code @FutureOrPresent} usa la zona horaria de la JVM y podría discrepar de los servicios sobre "hoy". */
    @Bean
    ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
