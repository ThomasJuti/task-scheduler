package com.jutinico.scheduler.worker;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jutinico.scheduler.skill.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int availabilityHoursPerDay;

    @ManyToMany
    @JoinTable(name = "worker_skill",
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    protected Worker() {
    }

    public Worker(String name, int availabilityHoursPerDay, Set<Skill> skills) {
        this.name = name;
        this.availabilityHoursPerDay = availabilityHoursPerDay;
        this.skills = new LinkedHashSet<>(skills);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAvailabilityHoursPerDay() {
        return availabilityHoursPerDay;
    }

    public void setAvailabilityHoursPerDay(int availabilityHoursPerDay) {
        this.availabilityHoursPerDay = availabilityHoursPerDay;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = new LinkedHashSet<>(skills);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
