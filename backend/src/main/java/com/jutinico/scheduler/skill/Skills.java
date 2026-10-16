package com.jutinico.scheduler.skill;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Helpers for the Req3a rule: a task's skills must be a subset of the worker's skills. */
public final class Skills {

    private Skills() {
    }

    /** Skills required by the task that the worker does not have, sorted by name (empty = compatible). */
    public static List<String> missing(Collection<Skill> required, Collection<Skill> available) {
        Set<String> availableKeys = available.stream().map(Skill::key).collect(Collectors.toSet());
        return required.stream()
                .filter(skill -> !availableKeys.contains(skill.key()))
                .map(Skill::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static boolean isSubset(Collection<Skill> required, Collection<Skill> available) {
        return missing(required, available).isEmpty();
    }

    public static List<String> names(Collection<Skill> skills) {
        return skills.stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
