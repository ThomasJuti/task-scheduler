package com.jutinico.scheduler.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query("select a from Assignment a order by a.startDate, a.id")
    List<Assignment> findAllWithWorker();

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query("select a from Assignment a where a.worker.id = :workerId order by a.startDate, a.id")
    List<Assignment> findByWorkerId(Long workerId);

    @EntityGraph(attributePaths = {"worker", "task"})
    Optional<Assignment> findByTaskId(Long taskId);

    @EntityGraph(attributePaths = {"worker", "task"})
    List<Assignment> findByTaskIdIn(Collection<Long> taskIds);
}
