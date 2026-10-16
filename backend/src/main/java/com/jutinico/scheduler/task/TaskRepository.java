package com.jutinico.scheduler.task;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "skills")
    List<Task> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "skills")
    List<Task> findByStatusOrderByIdAsc(TaskStatus status);

    /** Pending tasks with no assignment: the input of the auto-assignment. */
    @EntityGraph(attributePaths = "skills")
    @Query("""
            select t from Task t
            where t.status = com.jutinico.scheduler.task.TaskStatus.NOT_DONE
              and not exists (select 1 from Assignment a where a.task = t)
            """)
    List<Task> findPendingUnassigned();
}
