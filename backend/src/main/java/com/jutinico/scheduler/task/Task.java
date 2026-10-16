package com.jutinico.scheduler.task;

import java.util.LinkedHashSet;
import java.util.Set;

import com.jutinico.scheduler.skill.Skill;
import com.jutinico.scheduler.skill.Skills;
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

    public int getEffortHours() {
        return effortHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void updateDetails(String name, int effortHours, Set<Skill> skills, TaskStatus status) {
        this.name = name;
        this.effortHours = effortHours;
        this.skills = new LinkedHashSet<>(skills);
        this.status = status;
    }

    public void changeStatus(TaskStatus status) {
        this.status = status;
    }

    /** Verdadero si cambian el esfuerzo o los skills (sin distinguir mayúsculas); no se permite mientras esté asignada porque invalidaría la planificación. */
    public boolean wouldChangePlanning(int newEffortHours, Set<Skill> newSkills) {
        return effortHours != newEffortHours || !Skills.sameSkills(skills, newSkills);
    }
}
