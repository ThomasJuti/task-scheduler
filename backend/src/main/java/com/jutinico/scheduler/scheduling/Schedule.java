package com.jutinico.scheduler.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Result of planning a task: the first and last working day with hours, and the hours per day. */
public record Schedule(LocalDate startDate, LocalDate endDate, List<DailyAllocation> allocations) {

    /** Adds this schedule's hours to a worker's load map. */
    public void addTo(Map<LocalDate, Integer> usedHoursByDate) {
        allocations.forEach(a -> usedHoursByDate.merge(a.date(), a.hours(), Integer::sum));
    }
}
