package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class WorkerLoad {

    private WorkerLoad() {
    }

    static Map<LocalDate, Integer> usedHours(Collection<Assignment> assignments, Long excludedAssignmentId) {
        Map<LocalDate, Integer> used = new HashMap<>();
        assignments.stream()
                .filter(a -> !Objects.equals(a.getId(), excludedAssignmentId))
                .flatMap(a -> a.getAllocations().stream())
                .forEach(al -> used.merge(al.getWorkDate(), al.getHours(), Integer::sum));
        return used;
    }
}
