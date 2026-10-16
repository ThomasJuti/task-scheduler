package com.jutinico.scheduler.task;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jutinico.scheduler.skill.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int effortHours;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToMany
    @JoinTable(name = "task_skill",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    protected Task() {
    }

    public Task(String name, int effortHours, TaskStatus status, Set<Skill> skills) {
        this.name = name;
        this.effortHours = effortHours;
        this.status = status;
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

    public int getEffortHours() {
        return effortHours;
    }

    public void setEffortHours(int effortHours) {
        this.effortHours = effortHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
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
