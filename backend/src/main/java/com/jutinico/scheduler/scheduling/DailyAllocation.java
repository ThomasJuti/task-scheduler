package com.jutinico.scheduler.scheduling;

import java.time.LocalDate;

public record DailyAllocation(LocalDate date, int hours) {
}
