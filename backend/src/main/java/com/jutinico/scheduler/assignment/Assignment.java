package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.jutinico.scheduler.scheduling.Schedule;
import com.jutinico.scheduler.task.Task;
import com.jutinico.scheduler.worker.Worker;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;

@Entity
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    private LocalDate startDate;

    private LocalDate endDate;

    @ElementCollection
    @CollectionTable(name = "assignment_allocation", joinColumns = @JoinColumn(name = "assignment_id"))
    @OrderBy("workDate")
    private List<Allocation> allocations = new ArrayList<>();

    protected Assignment() {
    }

    public Assignment(Worker worker, Task task, Schedule schedule) {
        this.worker = worker;
        this.task = task;
        applySchedule(schedule);
    }

    public void reschedule(Worker newWorker, Schedule schedule) {
        this.worker = newWorker;
        applySchedule(schedule);
    }

    private void applySchedule(Schedule schedule) {
        this.startDate = schedule.startDate();
        this.endDate = schedule.endDate();
        this.allocations.clear();
        schedule.allocations().forEach(a -> this.allocations.add(new Allocation(a.date(), a.hours())));
    }

    public int totalHours() {
        return allocations.stream().mapToInt(Allocation::getHours).sum();
    }

    public Long getId() {
        return id;
    }

    public Worker getWorker() {
        return worker;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<Allocation> getAllocations() {
        return allocations;
    }
}
