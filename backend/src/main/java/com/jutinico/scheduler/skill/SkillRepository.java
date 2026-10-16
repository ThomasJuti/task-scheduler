package com.jutinico.scheduler.skill;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("select s from Skill s where lower(s.name) in :keys")
    List<Skill> findByKeys(Collection<String> keys);

    List<Skill> findAllByOrderByNameAsc();
}
