package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query(value = "select a from Assignment a", countQuery = "select count(a) from Assignment a")
    Page<Assignment> findPage(Pageable pageable);

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query(value = "select a from Assignment a where a.worker.id = :workerId",
            countQuery = "select count(a) from Assignment a where a.worker.id = :workerId")
    Page<Assignment> findPageByWorkerId(Long workerId, Pageable pageable);

    /**
     * Asignaciones que aún ocupan horas desde {@code from} (las que terminaron antes no afectan la planificación).
     * Evita cargar todo el historial para calcular la carga de los trabajadores.
     */
    @EntityGraph(attributePaths = {"worker", "task"})
    @Query("select a from Assignment a where a.endDate >= :from")
    List<Assignment> findActiveFrom(LocalDate from);

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query("select a from Assignment a where a.worker.id = :workerId and a.endDate >= :from")
    List<Assignment> findActiveByWorkerFrom(Long workerId, LocalDate from);

    @Query("""
            select new com.jutinico.scheduler.assignment.WorkerLoadSummary(a.worker.id, count(distinct a.id), coalesce(sum(al.hours), 0L))
            from Assignment a left join a.allocations al
            group by a.worker.id
            """)
    List<WorkerLoadSummary> summarizeLoadByWorker();

    @EntityGraph(attributePaths = {"worker", "task"})
    @Query("select a from Assignment a where a.worker.id = :workerId order by a.startDate, a.id")
    List<Assignment> findByWorkerId(Long workerId);

    @EntityGraph(attributePaths = {"worker", "task"})
    Optional<Assignment> findByTaskId(Long taskId);

    @EntityGraph(attributePaths = {"worker", "task"})
    List<Assignment> findByTaskIdIn(Collection<Long> taskIds);
}
