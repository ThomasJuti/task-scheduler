package com.jutinico.scheduler.worker;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @EntityGraph(attributePaths = "skills")
    List<Worker> findAllByOrderByIdAsc();
}
