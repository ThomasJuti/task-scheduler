package com.jutinico.scheduler.assignment;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignedItem;
import com.jutinico.scheduler.assignment.AssignmentDtos.UnassignedItem;
import com.jutinico.scheduler.scheduling.Schedule;
import com.jutinico.scheduler.scheduling.WorkCalendar;
import com.jutinico.scheduler.skill.Skills;
import com.jutinico.scheduler.task.Task;
import com.jutinico.scheduler.task.TaskRepository;
import com.jutinico.scheduler.worker.Worker;
import com.jutinico.scheduler.worker.WorkerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignación automática (Req4), voraz: primero las tareas más grandes (empate: más skills, luego id), cada una al
 * trabajador compatible que la termine antes (empate: menos horas reservadas desde la fecha de inicio, luego menor id). Las tareas que
 * nadie puede tomar se reportan con su motivo.
 */
@Service
public class AutoAssignmentService {

    private static final Comparator<Task> PRIORITY = Comparator
            .comparingInt(Task::getEffortHours).reversed()
            .thenComparing(Comparator.comparingInt((Task t) -> t.getSkills().size()).reversed())
            .thenComparing(Task::getId);

    private final AssignmentRepository assignments;
    private final WorkerRepository workers;
    private final TaskRepository tasks;
    private final Clock clock;

    public AutoAssignmentService(AssignmentRepository assignments, WorkerRepository workers, TaskRepository tasks,
                                 Clock clock) {
        this.assignments = assignments;
        this.workers = workers;
        this.tasks = tasks;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AutoAssignResponse run(LocalDate requestedStart, boolean dryRun) {
        LocalDate startDate = WorkCalendar.nextWorkingDay(requestedStart != null ? requestedStart : LocalDate.now(clock));
        // Una ejecución real bloquea a todos los trabajadores (por id) antes de leer su carga, para que una asignación
        // manual concurrente no reserve las mismas horas. Un dry run no guarda nada, así que no bloquea a nadie.
        List<Worker> allWorkers = dryRun ? workers.findAllByOrderByIdAsc() : workers.findAllForUpdate();
        Map<Long, WorkerBookings> bookings = loadByWorker(allWorkers, startDate);
        List<Task> pending = new ArrayList<>(dryRun ? tasks.findPendingUnassigned() : tasks.findPendingUnassignedForUpdate());
        pending.sort(PRIORITY);

        List<AutoAssignedItem> assigned = new ArrayList<>();
        List<UnassignedItem> unassigned = new ArrayList<>();
        for (Task task : pending) {
            Candidate best = pickBestWorker(task, allWorkers, bookings, startDate);
            if (best == null) {
                unassigned.add(new UnassignedItem(task.getId(), task.getName(), Skills.names(task.getSkills()),
                        "No worker has all the required skills " + Skills.names(task.getSkills())));
                continue;
            }
            Worker worker = best.worker();
            bookings.get(worker.getId()).book(best.schedule(), task.getEffortHours());
            if (!dryRun) {
                assignments.save(new Assignment(worker, task, best.schedule()));
            }
            assigned.add(new AutoAssignedItem(task.getId(), task.getName(), task.getEffortHours(), worker.getId(),
                    worker.getName(), best.schedule().startDate(), best.schedule().endDate()));
        }
        return new AutoAssignResponse(dryRun, startDate, assigned, unassigned);
    }

    private Map<Long, WorkerBookings> loadByWorker(List<Worker> allWorkers, LocalDate startDate) {
        Map<Long, List<Assignment>> assignmentsByWorker = assignments.findActiveFrom(startDate).stream()
                .collect(Collectors.groupingBy(a -> a.getWorker().getId()));
        Map<Long, WorkerBookings> bookings = new HashMap<>();
        for (Worker worker : allWorkers) {
            bookings.put(worker.getId(), new WorkerBookings(assignmentsByWorker.getOrDefault(worker.getId(), List.of())));
        }
        return bookings;
    }

    private static Candidate pickBestWorker(Task task, List<Worker> allWorkers, Map<Long, WorkerBookings> bookings,
                                            LocalDate startDate) {
        Candidate best = null;
        for (Worker worker : allWorkers) {
            if (!Skills.isSubset(task.getSkills(), worker.getSkills())) {
                continue;
            }
            WorkerBookings booked = bookings.get(worker.getId());
            Schedule schedule = WorkCalendar.plan(worker.getAvailabilityHoursPerDay(), booked.usedHoursByDate,
                    startDate, task.getEffortHours());
            Candidate candidate = new Candidate(worker, schedule, booked.assignedHours);
            if (best == null || candidate.isBetterThan(best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static final class WorkerBookings {

        private final Map<LocalDate, Integer> usedHoursByDate;
        private int assignedHours;

        WorkerBookings(List<Assignment> current) {
            this.usedHoursByDate = WorkerLoad.usedHours(current, null);
            this.assignedHours = current.stream().mapToInt(Assignment::totalHours).sum();
        }

        void book(Schedule schedule, int hours) {
            schedule.addTo(usedHoursByDate);
            assignedHours += hours;
        }
    }

    private record Candidate(Worker worker, Schedule schedule, int assignedHours) {

        boolean isBetterThan(Candidate other) {
            int byEnd = schedule.endDate().compareTo(other.schedule.endDate());
            if (byEnd != 0) {
                return byEnd < 0;
            }
            if (assignedHours != other.assignedHours) {
                return assignedHours < other.assignedHours;
            }
            return worker.getId() < other.worker.getId();
        }
    }
}
