package com.jutinico.scheduler.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record Schedule(LocalDate startDate, LocalDate endDate, List<DailyAllocation> allocations) {

    public void addTo(Map<LocalDate, Integer> usedHoursByDate) {
        allocations.forEach(a -> usedHoursByDate.merge(a.date(), a.hours(), Integer::sum));
    }
}
