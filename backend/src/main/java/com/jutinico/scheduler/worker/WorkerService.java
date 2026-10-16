package com.jutinico.scheduler.worker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.Assignment;
import com.jutinico.scheduler.assignment.AssignmentRepository;
import com.jutinico.scheduler.common.ConflictException;
import com.jutinico.scheduler.common.Names;
import com.jutinico.scheduler.common.NotFoundException;
import com.jutinico.scheduler.skill.Skill;
import com.jutinico.scheduler.skill.SkillService;
import com.jutinico.scheduler.skill.Skills;
import com.jutinico.scheduler.worker.WorkerDtos.WorkerRequest;
import com.jutinico.scheduler.worker.WorkerDtos.WorkerResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerService {

    private final WorkerRepository workers;
    private final AssignmentRepository assignments;
    private final SkillService skillService;

    public WorkerService(WorkerRepository workers, AssignmentRepository assignments, SkillService skillService) {
        this.workers = workers;
        this.assignments = assignments;
        this.skillService = skillService;
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> list() {
        Map<Long, List<Assignment>> byWorker = assignments.findAllWithWorker().stream()
                .collect(Collectors.groupingBy(a -> a.getWorker().getId()));
        return workers.findAllByOrderByIdAsc().stream()
                .map(w -> toResponse(w, byWorker.getOrDefault(w.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkerResponse get(Long id) {
        return toResponse(find(id), assignments.findByWorkerId(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WorkerResponse create(WorkerRequest request) {
        Worker worker = new Worker(Names.normalize(request.name()), request.availabilityHoursPerDay(),
                skillService.resolve(request.skills()));
        return toResponse(workers.save(worker), List.of());
    }

    /**
     * Al cambiar la disponibilidad se conservan las planificaciones existentes (eran válidas al planificar); las nuevas
     * asignaciones usan la nueva. Se rechaza quitar un skill que necesita una tarea asignada.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WorkerResponse update(Long id, WorkerRequest request) {
        Worker worker = find(id);
        Set<Skill> newSkills = skillService.resolve(request.skills());
        List<Assignment> current = assignments.findByWorkerId(id);
        for (Assignment assignment : current) {
            List<String> missing = Skills.missing(assignment.getTask().getSkills(), newSkills);
            if (!missing.isEmpty()) {
                throw new ConflictException("Worker is assigned to task '" + assignment.getTask().getName()
                        + "', which requires " + missing + "; unassign it before removing those skills");
            }
        }
        worker.updateDetails(Names.normalize(request.name()), request.availabilityHoursPerDay(), newSkills);
        return toResponse(worker, current);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        Worker worker = find(id);
        assignments.deleteAll(assignments.findByWorkerId(id));
        workers.delete(worker);
    }

    private Worker find(Long id) {
        return workers.findById(id).orElseThrow(() -> NotFoundException.of("Worker", id));
    }

    private static WorkerResponse toResponse(Worker worker, List<Assignment> assigned) {
        int hours = assigned.stream().mapToInt(Assignment::totalHours).sum();
        return new WorkerResponse(worker.getId(), worker.getName(), worker.getAvailabilityHoursPerDay(),
                Skills.names(worker.getSkills()), assigned.size(), hours);
    }
}
