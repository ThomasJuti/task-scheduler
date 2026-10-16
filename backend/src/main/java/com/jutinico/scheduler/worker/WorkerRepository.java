package com.jutinico.scheduler.worker;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @EntityGraph(attributePaths = "skills")
    List<Worker> findAllByOrderByIdAsc();

    /** {@code SELECT ... FOR UPDATE}: serializa asignaciones concurrentes al mismo trabajador para que no reserven las mismas horas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Worker w where w.id = :id")
    Optional<Worker> findByIdForUpdate(Long id);

    /** Bloquea a todos los trabajadores como {@link #findByIdForUpdate}, siempre por id para evitar interbloqueos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "skills")
    @Query("select w from Worker w order by w.id")
    List<Worker> findAllForUpdate();
}
