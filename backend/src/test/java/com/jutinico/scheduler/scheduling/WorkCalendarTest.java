package com.jutinico.scheduler.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkCalendarTest {

    // 2026-10-19 es lunes
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate FRIDAY = MONDAY.plusDays(4);
    private static final LocalDate SATURDAY = MONDAY.plusDays(5);
    private static final LocalDate NEXT_MONDAY = MONDAY.plusDays(7);

    @Test
    void weekendsAreNotWorkingDays() {
        assertThat(WorkCalendar.isWorkingDay(FRIDAY)).isTrue();
        assertThat(WorkCalendar.isWorkingDay(SATURDAY)).isFalse();
        assertThat(WorkCalendar.isWorkingDay(SATURDAY.plusDays(1))).isFalse();
        assertThat(WorkCalendar.nextWorkingDay(SATURDAY)).isEqualTo(NEXT_MONDAY);
        assertThat(WorkCalendar.nextWorkingDay(MONDAY)).isEqualTo(MONDAY);
    }

    @Test
    void taskThatFitsInOneDayStartsAndEndsTheSameDay() {
        Schedule schedule = WorkCalendar.plan(8, Map.of(), MONDAY, 6);

        assertThat(schedule.startDate()).isEqualTo(MONDAY);
        assertThat(schedule.endDate()).isEqualTo(MONDAY);
        assertThat(schedule.allocations()).containsExactly(new DailyAllocation(MONDAY, 6));
    }

    @Test
    void effortIsSpreadOverWorkingDaysSkippingTheWeekend() {
        // 4 h/día, 10 h desde el jueves: jue 4, vie 4, (fin de semana), lun 2
        LocalDate thursday = MONDAY.plusDays(3);
        Schedule schedule = WorkCalendar.plan(4, Map.of(), thursday, 10);

        assertThat(schedule.allocations()).containsExactly(
                new DailyAllocation(thursday, 4),
                new DailyAllocation(FRIDAY, 4),
                new DailyAllocation(NEXT_MONDAY, 2));
        assertThat(schedule.endDate()).isEqualTo(NEXT_MONDAY);
    }

    @Test
    void startOnWeekendMovesToMonday() {
        Schedule schedule = WorkCalendar.plan(8, Map.of(), SATURDAY, 3);

        assertThat(schedule.startDate()).isEqualTo(NEXT_MONDAY);
    }

    @Test
    void existingLoadOnlyLeavesTheFreeHours() {
        // Lunes completo, el martes tiene 5 de 8 horas reservadas
        Map<LocalDate, Integer> used = Map.of(MONDAY, 8, MONDAY.plusDays(1), 5);
        Schedule schedule = WorkCalendar.plan(8, used, MONDAY, 7);

        assertThat(schedule.allocations()).containsExactly(
                new DailyAllocation(MONDAY.plusDays(1), 3),
                new DailyAllocation(MONDAY.plusDays(2), 4));
        assertThat(schedule.startDate()).isEqualTo(MONDAY.plusDays(1));
    }

    @Test
    void addToAccumulatesTheLoad() {
        Map<LocalDate, Integer> used = new HashMap<>(Map.of(MONDAY, 2));
        WorkCalendar.plan(8, used, MONDAY, 10).addTo(used);

        assertThat(used).containsEntry(MONDAY, 8).containsEntry(MONDAY.plusDays(1), 4);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> WorkCalendar.plan(0, Map.of(), MONDAY, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WorkCalendar.plan(8, Map.of(), MONDAY, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
