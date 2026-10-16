package com.jutinico.scheduler.scheduling;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure scheduling logic: working days are Monday to Friday and a worker can spend at most
 * {@code availabilityPerDay} hours per working day across all of their assignments.
 */
public final class WorkCalendar {

    private WorkCalendar() {
    }

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /** Returns {@code date} itself when it is a working day, otherwise the following Monday. */
    public static LocalDate nextWorkingDay(LocalDate date) {
        LocalDate current = date;
        while (!isWorkingDay(current)) {
            current = current.plusDays(1);
        }
        return current;
    }

    /**
     * Plans {@code effortHours} of work starting at {@code startDate}, filling the free capacity of each
     * working day (availability minus hours already used by other assignments).
     *
     * @param usedHoursByDate hours already booked for the worker, by date (not modified)
     */
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
