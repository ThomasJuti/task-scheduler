package com.jutinico.scheduler.admin;

import com.jutinico.scheduler.assignment.AssignmentRepository;
import com.jutinico.scheduler.common.NotFoundException;
import com.jutinico.scheduler.skill.SkillRepository;
import com.jutinico.scheduler.task.TaskRepository;
import com.jutinico.scheduler.worker.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final boolean resetEnabled;
    private final AssignmentRepository assignments;
    private final TaskRepository tasks;
    private final WorkerRepository workers;
    private final SkillRepository skills;

    public AdminService(@Value("${app.admin.reset-enabled}") boolean resetEnabled, AssignmentRepository assignments,
                        TaskRepository tasks, WorkerRepository workers, SkillRepository skills) {
        this.resetEnabled = resetEnabled;
        this.assignments = assignments;
        this.tasks = tasks;
        this.workers = workers;
        this.skills = skills;
    }

    /** Deletes all business data (not the user accounts). Used by the seed script; disabled unless configured. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reset() {
        if (!resetEnabled) {
            // 404 rather than 403: when disabled the endpoint should look like it does not exist at all
            throw new NotFoundException("Not found");
        }
        assignments.deleteAllInBatch();
        tasks.deleteAllInBatch();
        workers.deleteAllInBatch();
        skills.deleteAllInBatch();
    }
}
