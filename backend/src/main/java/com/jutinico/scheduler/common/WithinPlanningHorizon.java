package com.jutinico.scheduler.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * La fecha no puede pasar de {@link Limits#PLANNING_HORIZON_YEARS} años desde hoy. Sin límite se aceptaban fechas
 * absurdas (año 10003) y la fecha máxima de Java desbordaba el calendario con un 500.
 */
@Documented
@Constraint(validatedBy = WithinPlanningHorizon.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithinPlanningHorizon {

    String message() default "must be within " + Limits.PLANNING_HORIZON_YEARS + " years from today";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<WithinPlanningHorizon, LocalDate> {

        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            // Mismo reloj que @FutureOrPresent (el de APP_TIMEZONE, ver ClockConfig)
            LocalDate today = LocalDate.now(context.getClockProvider().getClock());
            return !value.isAfter(today.plusYears(Limits.PLANNING_HORIZON_YEARS));
        }
    }
}
