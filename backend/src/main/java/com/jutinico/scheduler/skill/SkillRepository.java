package com.jutinico.scheduler.skill;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("select s from Skill s where lower(s.name) in :keys")
    List<Skill> findByKeys(Collection<String> keys);

    List<Skill> findAllByOrderByNameAsc();

    /**
     * Crea el skill si no existe. Si otra petición lo está creando a la vez, PostgreSQL espera a que termine y no
     * hace nada, en vez de fallar por el índice único (que daría un 409 a una petición válida).
     */
    @Modifying
    @Query(value = "insert into skill (name) values (:name) on conflict do nothing", nativeQuery = true)
    void insertIfAbsent(String name);
}
