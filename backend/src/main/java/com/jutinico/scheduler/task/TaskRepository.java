package com.jutinico.scheduler.task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "skills")
    List<Task> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "skills")
    List<Task> findByStatusOrderByIdAsc(TaskStatus status);

    @EntityGraph(attributePaths = "skills")
    @Query("""
            select t from Task t
            where t.status = com.jutinico.scheduler.task.TaskStatus.NOT_DONE
              and not exists (select 1 from Assignment a where a.task = t)
            """)
    List<Task> findPendingUnassigned();

    /**
     * Bloqueos de fila ({@code SELECT ... FOR UPDATE}) hasta el fin de la transacción, para que editar una tarea y
     * asignarla no se crucen. Orden de bloqueo en toda la app: primero trabajadores, luego tareas por id.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id in :ids order by t.id")
    List<Task> findAllByIdForUpdate(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Task t
            where t.status = com.jutinico.scheduler.task.TaskStatus.NOT_DONE
              and not exists (select 1 from Assignment a where a.task = t)
            order by t.id
            """)
    List<Task> findPendingUnassignedForUpdate();
}
