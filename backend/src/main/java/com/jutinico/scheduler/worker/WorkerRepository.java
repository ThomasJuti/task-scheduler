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

    /**
     * Loads a worker with a row lock ({@code SELECT ... FOR UPDATE}) held until the transaction ends. Taken
     * before reading the worker's load, so two concurrent assignments to the same worker are serialized and
     * cannot both book the same free hours.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Worker w where w.id = :id")
    Optional<Worker> findByIdForUpdate(Long id);

    /** All workers (with skills) locked like {@link #findByIdForUpdate}; always in id order to avoid deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "skills")
    @Query("select w from Worker w order by w.id")
    List<Worker> findAllForUpdate();
}
