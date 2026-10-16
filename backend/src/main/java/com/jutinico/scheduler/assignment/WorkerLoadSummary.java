package com.jutinico.scheduler.assignment;

/** Tareas y horas asignadas a un trabajador, calculadas con un GROUP BY en vez de cargar todas sus asignaciones. */
public record WorkerLoadSummary(Long workerId, Long assignedTasks, Long assignedHours) {
}
