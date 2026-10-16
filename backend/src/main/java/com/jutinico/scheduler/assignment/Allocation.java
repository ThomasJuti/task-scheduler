package com.jutinico.scheduler.assignment;

import java.time.LocalDate;

import jakarta.persistence.Embeddable;

@Embeddable
public class Allocation {

    private LocalDate workDate;

    private int hours;

    protected Allocation() {
    }

    public Allocation(LocalDate workDate, int hours) {
        this.workDate = workDate;
        this.hours = hours;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public int getHours() {
        return hours;
    }
}
