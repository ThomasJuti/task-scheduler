package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.AssignmentDtos.AllocationResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.AssignRequest;
import com.jutinico.scheduler.assignment.AssignmentDtos.AssignmentResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.ReassignRequest;
import com.jutinico.scheduler.common.BusinessRuleException;
import com.jutinico.scheduler.common.ConflictException;
import com.jutinico.scheduler.common.NotFoundException;
import com.jutinico.scheduler.scheduling.Schedule;
import com.jutinico.scheduler.scheduling.WorkCalendar;
import com.jutinico.scheduler.skill.Skills;
import com.jutinico.scheduler.task.Task;
import com.jutinico.scheduler.task.TaskRepository;
import com.jutinico.scheduler.task.TaskStatus;
import com.jutinico.scheduler.worker.Worker;
import com.jutinico.scheduler.worker.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Asignación manual (Req3). Aplica la regla de skills (Req3a): los skills de la tarea deben ser un subconjunto de los del trabajador. */
@Service
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final WorkerRepository workers;
    private final TaskRepository tasks;

    public AssignmentService(AssignmentRepository assignments, WorkerRepository workers, TaskRepository tasks) {
        this.assignments = assignments;
        this.workers = workers;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long workerId) {
        List<Assignment> found = workerId == null ? assignments.findAllWithWorker() : assignments.findByWorkerId(workerId);
        return found.stream().map(AssignmentService::toResponse).toList();
    }

    /** Todo o nada: si una tarea no es válida no se guarda ninguna. Las tareas se planifican en el orden recibido. */
    @Transactional
    public List<AssignmentResponse> assign(AssignRequest request) {
        Worker worker = lockWorker(request.workerId());
        List<Task> requested = findTasksInOrder(request.taskIds());

        Map<Long, Assignment> existing = assignments.findByTaskIdIn(request.taskIds()).stream()
                .collect(Collectors.toMap(a -> a.getTask().getId(), Function.identity()));
        for (Task task : requested) {
            if (task.getStatus() == TaskStatus.DONE) {
                throw new ConflictException("Task '" + task.getName() + "' is already done");
            }
            Assignment current = existing.get(task.getId());
            if (current != null) {
                throw new ConflictException("Task '" + task.getName() + "' is already assigned to "
                        + current.getWorker().getName());
            }
            requireSkills(worker, task);
        }

        Map<LocalDate, Integer> used = WorkerLoad.usedHours(assignments.findByWorkerId(worker.getId()), null);
        List<AssignmentResponse> created = new ArrayList<>();
        for (Task task : requested) {
            Schedule schedule = WorkCalendar.plan(worker.getAvailabilityHoursPerDay(), used, request.startDate(),
                    task.getEffortHours());
            schedule.addTo(used);
            created.add(toResponse(assignments.save(new Assignment(worker, task, schedule))));
        }
        return created;
    }

    /** Solo se bloquea al trabajador destino: liberar horas del anterior no puede sobrecargar a nadie. */
    @Transactional
    public AssignmentResponse reassign(Long id, ReassignRequest request) {
        Assignment assignment = assignments.findById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
        Task task = assignment.getTask();
        if (task.getStatus() == TaskStatus.DONE) {
            throw new ConflictException("Task '" + task.getName() + "' is already done and cannot be rescheduled");
        }
        Worker worker = lockWorker(request.workerId());
        requireSkills(worker, task);

        Map<LocalDate, Integer> used = WorkerLoad.usedHours(assignments.findByWorkerId(worker.getId()), assignment.getId());
        Schedule schedule = WorkCalendar.plan(worker.getAvailabilityHoursPerDay(), used, request.startDate(),
                task.getEffortHours());
        assignment.reschedule(worker, schedule);
        return toResponse(assignment);
    }

    @Transactional
    public void unassign(Long id) {
        Assignment assignment = assignments.findById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
        assignments.delete(assignment);
    }

    /** Bloquea la fila del trabajador hasta el commit; si no, dos peticiones concurrentes podrían reservar las mismas horas libres. */
    private Worker lockWorker(Long id) {
        return workers.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("Worker", id));
    }

    private List<Task> findTasksInOrder(List<Long> ids) {
        Map<Long, Task> byId = tasks.findAllById(ids).stream().collect(Collectors.toMap(Task::getId, Function.identity()));
        Set<Long> missing = ids.stream().filter(id -> !byId.containsKey(id)).collect(Collectors.toCollection(TreeSet::new));
        if (!missing.isEmpty()) {
            throw new NotFoundException("Tasks not found: " + missing);
        }
        return ids.stream().map(byId::get).toList();
    }

    private static void requireSkills(Worker worker, Task task) {
        List<String> missing = Skills.missing(task.getSkills(), worker.getSkills());
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Worker '" + worker.getName() + "' cannot take task '" + task.getName()
                    + "': missing skills " + missing);
        }
    }

    static AssignmentResponse toResponse(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getWorker().getId(), a.getWorker().getName(), a.getTask().getId(),
                a.getTask().getName(), a.getTask().getEffortHours(), a.getTask().getStatus(), a.getStartDate(),
                a.getEndDate(), a.getAllocations().stream()
                        .map(al -> new AllocationResponse(al.getWorkDate(), al.getHours())).toList());
    }
}
