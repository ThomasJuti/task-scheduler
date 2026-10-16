package com.jutinico.scheduler.scheduling;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lógica de planificación pura: los días laborales son de lunes a viernes y un trabajador puede dedicar como máximo
 * {@code availabilityPerDay} horas por día laboral entre todas sus asignaciones.
 */
public final class WorkCalendar {

    private WorkCalendar() {
    }

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    public static LocalDate nextWorkingDay(LocalDate date) {
        LocalDate current = date;
        while (!isWorkingDay(current)) {
            current = current.plusDays(1);
        }
        return current;
    }

    /** Planifica {@code effortHours} desde {@code startDate} sobre la capacidad libre de cada día laboral; {@code usedHoursByDate} no se modifica. */
    public static Schedule plan(int availabilityPerDay, Map<LocalDate, Integer> usedHoursByDate,
                                LocalDate startDate, int effortHours) {
        if (availabilityPerDay < 1) {
            throw new IllegalArgumentException("availabilityPerDay must be at least 1");
        }
        if (effortHours < 1) {
            throw new IllegalArgumentException("effortHours must be at least 1");
        }

        List<DailyAllocation> allocations = new ArrayList<>();
        int remaining = effortHours;
        LocalDate day = nextWorkingDay(startDate);
        while (remaining > 0) {
            int used = usedHoursByDate.getOrDefault(day, 0);
            int free = Math.max(0, availabilityPerDay - used);
            if (free > 0) {
                int hours = Math.min(free, remaining);
                allocations.add(new DailyAllocation(day, hours));
                remaining -= hours;
            }
            if (remaining > 0) {
                day = nextWorkingDay(day.plusDays(1));
            }
        }
        return new Schedule(allocations.getFirst().date(), allocations.getLast().date(), List.copyOf(allocations));
    }
}
