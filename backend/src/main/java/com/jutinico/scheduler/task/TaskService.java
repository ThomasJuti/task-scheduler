package com.jutinico.scheduler.task;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.Assignment;
import com.jutinico.scheduler.assignment.AssignmentRepository;
import com.jutinico.scheduler.common.ConflictException;
import com.jutinico.scheduler.common.Names;
import com.jutinico.scheduler.common.PageResponse;
import com.jutinico.scheduler.common.NotFoundException;
import com.jutinico.scheduler.skill.Skill;
import com.jutinico.scheduler.skill.SkillService;
import com.jutinico.scheduler.skill.Skills;
import com.jutinico.scheduler.task.TaskDtos.AssignmentSummary;
import com.jutinico.scheduler.task.TaskDtos.TaskRequest;
import com.jutinico.scheduler.task.TaskDtos.TaskResponse;
import com.jutinico.scheduler.task.TaskDtos.TaskStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final AssignmentRepository assignments;
    private final SkillService skillService;

    public TaskService(TaskRepository tasks, AssignmentRepository assignments, SkillService skillService) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.skillService = skillService;
    }

    /**
     * Una página de tareas filtrada y ordenada en la base de datos. Solo se cargan las asignaciones de las tareas
     * de esa página, no la tabla entera.
     *
     * @param sort "campo" o "campo,asc|desc"; campos: id, name, effortHours, status (el controller los valida)
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(TaskStatus status, Boolean unassigned, String search, int page, int size,
                                           String sort) {
        Specification<Task> filter = TaskSpecifications.all();
        if (status != null) {
            filter = filter.and(TaskSpecifications.hasStatus(status));
        }
        if (unassigned != null) {
            filter = filter.and(TaskSpecifications.unassigned(unassigned));
        }
        if (search != null && !search.isBlank()) {
            filter = filter.and(TaskSpecifications.matches(search));
        }
        Page<Task> found = tasks.findAll(filter, PageRequest.of(page, size, toSort(sort)));

        List<Long> ids = found.getContent().stream().map(Task::getId).toList();
        Map<Long, Assignment> byTask = ids.isEmpty() ? Map.of() : assignments.findByTaskIdIn(ids).stream()
                .collect(Collectors.toMap(a -> a.getTask().getId(), Function.identity()));
        return PageResponse.of(found, task -> toResponse(task, byTask.get(task.getId())));
    }

    @Transactional(readOnly = true)
    public TaskStats stats() {
        long done = tasks.countByStatus(TaskStatus.DONE);
        long pending = tasks.countByStatus(TaskStatus.NOT_DONE);
        return new TaskStats(done + pending, pending, done, tasks.countPendingUnassigned());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        Task task = find(id);
        return toResponse(task, assignments.findByTaskId(id).orElse(null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(Names.normalize(request.name()), request.effortHours(), request.status(),
                skillService.resolve(request.skills()));
        return toResponse(tasks.save(task), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = lock(id);
        Assignment assignment = assignments.findByTaskId(id).orElse(null);
        Set<Skill> newSkills = skillService.resolve(request.skills());

        if (assignment != null && task.wouldChangePlanning(request.effortHours(), newSkills)) {
            throw new ConflictException("Task '" + task.getName() + "' is assigned to "
                    + assignment.getWorker().getName() + "; unassign it before changing its effort or skills");
        }
        task.updateDetails(Names.normalize(request.name()), request.effortHours(), newSkills, request.status());
        return toResponse(task, assignment);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = lock(id);
        task.changeStatus(status);
        return toResponse(task, assignments.findByTaskId(id).orElse(null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        Task task = find(id);
        assignments.findByTaskId(id).ifPresent(assignments::delete);
        tasks.delete(task);
    }

    private Task find(Long id) {
        return tasks.findById(id).orElseThrow(() -> NotFoundException.of("Task", id));
    }

    /** Espera a que termine una asignación en curso de esta tarea antes de comprobar si está asignada. */
    private Task lock(Long id) {
        return tasks.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("Task", id));
    }

    /** El id desempata para que el orden entre páginas sea estable; el nombre se ordena sin distinguir mayúsculas. */
    private static Sort toSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
        Sort.Order order = new Sort.Order(direction, parts[0]);
        if (parts[0].equals("name")) {
            order = order.ignoreCase();
        }
        return parts[0].equals("id") ? Sort.by(order) : Sort.by(order, Sort.Order.asc("id"));
    }


    static TaskResponse toResponse(Task task, Assignment assignment) {
        AssignmentSummary summary = assignment == null ? null : new AssignmentSummary(assignment.getId(),
                assignment.getWorker().getId(), assignment.getWorker().getName(),
                assignment.getStartDate(), assignment.getEndDate());
        return new TaskResponse(task.getId(), task.getName(), task.getEffortHours(), Skills.names(task.getSkills()),
                task.getStatus(), summary);
    }
}
