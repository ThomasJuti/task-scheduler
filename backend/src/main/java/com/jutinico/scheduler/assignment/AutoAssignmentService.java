package com.jutinico.scheduler.assignment;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Req4 — greedy, capacity-aware auto-assignment.
 * <ol>
 *   <li>Take every NOT_DONE task without assignment, biggest effort first (ties: more skills, then id),
 *       because big and specialised tasks are the hardest to place.</li>
 *   <li>Candidates are the workers whose skills contain all the task skills (Req3a).</li>
 *   <li>For each candidate, simulate the schedule on its current load (including what this run already
 *       assigned) and pick the one that finishes earliest; ties go to the worker with fewer assigned hours,
 *       then the lowest id.</li>
 *   <li>Tasks without any compatible worker are reported with the reason.</li>
 * </ol>
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
        List<Worker> allWorkers = workers.findAllByOrderByIdAsc();
        List<Task> pending = new ArrayList<>(tasks.findPendingUnassigned());
        pending.sort(PRIORITY);

        Map<Long, Map<LocalDate, Integer>> loadByWorker = new HashMap<>();
        Map<Long, Integer> hoursByWorker = new HashMap<>();
        List<Assignment> current = assignments.findAllWithWorker();
        for (Worker worker : allWorkers) {
            List<Assignment> own = current.stream().filter(a -> a.getWorker().getId().equals(worker.getId())).toList();
            loadByWorker.put(worker.getId(), WorkerLoad.usedHours(own, null));
            hoursByWorker.put(worker.getId(), own.stream().mapToInt(Assignment::totalHours).sum());
        }

        List<AutoAssignedItem> assigned = new ArrayList<>();
        List<UnassignedItem> unassigned = new ArrayList<>();
        for (Task task : pending) {
            Candidate best = null;
            for (Worker worker : allWorkers) {
                if (!Skills.isSubset(task.getSkills(), worker.getSkills())) {
                    continue;
                }
                Schedule schedule = WorkCalendar.plan(worker.getAvailabilityHoursPerDay(),
                        loadByWorker.get(worker.getId()), startDate, task.getEffortHours());
                Candidate candidate = new Candidate(worker, schedule, hoursByWorker.get(worker.getId()));
                if (best == null || candidate.isBetterThan(best)) {
                    best = candidate;
                }
            }
            if (best == null) {
                unassigned.add(new UnassignedItem(task.getId(), task.getName(), Skills.names(task.getSkills()),
                        "No worker has all the required skills " + Skills.names(task.getSkills())));
                continue;
            }
            Worker worker = best.worker();
            best.schedule().addTo(loadByWorker.get(worker.getId()));
            hoursByWorker.merge(worker.getId(), task.getEffortHours(), Integer::sum);
            if (!dryRun) {
                assignments.save(new Assignment(worker, task, best.schedule()));
            }
            assigned.add(new AutoAssignedItem(task.getId(), task.getName(), task.getEffortHours(), worker.getId(),
                    worker.getName(), best.schedule().startDate(), best.schedule().endDate()));
        }
        return new AutoAssignResponse(dryRun, startDate, assigned, unassigned);
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
