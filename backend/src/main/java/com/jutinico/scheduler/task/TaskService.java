package com.jutinico.scheduler.task;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.Assignment;
import com.jutinico.scheduler.assignment.AssignmentRepository;
import com.jutinico.scheduler.common.ConflictException;
import com.jutinico.scheduler.common.Names;
import com.jutinico.scheduler.common.NotFoundException;
import com.jutinico.scheduler.skill.Skill;
import com.jutinico.scheduler.skill.SkillService;
import com.jutinico.scheduler.skill.Skills;
import com.jutinico.scheduler.task.TaskDtos.AssignmentSummary;
import com.jutinico.scheduler.task.TaskDtos.TaskRequest;
import com.jutinico.scheduler.task.TaskDtos.TaskResponse;
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

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status, Boolean unassigned) {
        List<Task> found = status == null ? tasks.findAllByOrderByIdAsc() : tasks.findByStatusOrderByIdAsc(status);
        Map<Long, Assignment> byTask = assignments.findAllWithWorker().stream()
                .collect(Collectors.toMap(a -> a.getTask().getId(), Function.identity()));
        return found.stream()
                .filter(t -> unassigned == null || unassigned != byTask.containsKey(t.getId()))
                .map(t -> toResponse(t, byTask.get(t.getId())))
                .toList();
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
        Task task = find(id);
        Assignment assignment = assignments.findByTaskId(id).orElse(null);
        var newSkills = skillService.resolve(request.skills());

        if (assignment != null) {
            boolean effortChanged = task.getEffortHours() != request.effortHours();
            boolean skillsChanged = !sameSkills(task.getSkills(), newSkills);
            if (effortChanged || skillsChanged) {
                throw new ConflictException("Task '" + task.getName() + "' is assigned to "
                        + assignment.getWorker().getName() + "; unassign it before changing its effort or skills");
            }
        }
        task.setName(Names.normalize(request.name()));
        task.setEffortHours(request.effortHours());
        task.setSkills(newSkills);
        task.setStatus(request.status());
        return toResponse(task, assignment);
    }

    /** Allowed for USER and ADMIN: only the status changes, the task fields stay untouched. */
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = find(id);
        task.setStatus(status);
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

    private static boolean sameSkills(java.util.Set<Skill> a, java.util.Set<Skill> b) {
        return Skills.isSubset(a, b) && Skills.isSubset(b, a);
    }

    static TaskResponse toResponse(Task task, Assignment assignment) {
        AssignmentSummary summary = assignment == null ? null : new AssignmentSummary(assignment.getId(),
                assignment.getWorker().getId(), assignment.getWorker().getName(),
                assignment.getStartDate(), assignment.getEndDate());
        return new TaskResponse(task.getId(), task.getName(), task.getEffortHours(), Skills.names(task.getSkills()),
                task.getStatus(), summary);
    }
}
