package com.jutinico.scheduler.task;

import java.util.Locale;

import com.jutinico.scheduler.assignment.Assignment;
import com.jutinico.scheduler.skill.Skill;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de tareas, resueltos en la base de datos (no en memoria). */
final class TaskSpecifications {

    private TaskSpecifications() {
    }

    static Specification<Task> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** {@code true}: solo sin asignación; {@code false}: solo asignadas. */
    static Specification<Task> unassigned(boolean unassigned) {
        return (root, query, cb) -> {
            Subquery<Long> assignment = query.subquery(Long.class);
            Root<Assignment> a = assignment.from(Assignment.class);
            assignment.select(a.get("id")).where(cb.equal(a.get("task"), root));
            return unassigned ? cb.not(cb.exists(assignment)) : cb.exists(assignment);
        };
    }

    /** El texto aparece (sin distinguir mayúsculas) en el nombre, en una skill o en el trabajador asignado. */
    static Specification<Task> matches(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Subquery<Long> skill = query.subquery(Long.class);
            Root<Task> sameTask = skill.correlate(root);
            Join<Task, Skill> s = sameTask.join("skills");
            skill.select(s.get("id")).where(cb.like(cb.lower(s.get("name")), pattern, '\\'));

            Subquery<Long> worker = query.subquery(Long.class);
            Root<Assignment> a = worker.from(Assignment.class);
            worker.select(a.get("id")).where(cb.equal(a.get("task"), root),
                    cb.like(cb.lower(a.get("worker").get("name")), pattern, '\\'));

            return cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'), cb.exists(skill), cb.exists(worker));
        };
    }

    /** Escapa los comodines de LIKE para que "%" o "_" se busquen literalmente. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
