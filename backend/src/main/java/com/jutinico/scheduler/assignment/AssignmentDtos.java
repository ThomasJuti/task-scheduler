package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.List;

import com.jutinico.scheduler.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    /** Req3: manual assignment of one or more tasks to a worker from a start date. */
    public record AssignRequest(
            @NotNull Long workerId,
            @NotEmpty @Size(max = 100) @UniqueElements List<@NotNull Long> taskIds,
            @NotNull @FutureOrPresent LocalDate startDate) {
    }

    /** Moves an assignment to another worker and/or start date. */
    public record ReassignRequest(
            @NotNull Long workerId,
            @NotNull @FutureOrPresent LocalDate startDate) {
    }

    /** Req4: auto-assignment. {@code startDate} defaults to today; {@code dryRun} only previews the result. */
    public record AutoAssignRequest(@FutureOrPresent LocalDate startDate, Boolean dryRun) {
    }

    public record AllocationResponse(LocalDate date, int hours) {
    }

    public record AssignmentResponse(Long id, Long workerId, String workerName, Long taskId, String taskName,
                                     int effortHours, TaskStatus taskStatus, LocalDate startDate, LocalDate endDate,
                                     List<AllocationResponse> allocations) {
    }

    public record AutoAssignedItem(Long taskId, String taskName, int effortHours, Long workerId, String workerName,
                                   LocalDate startDate, LocalDate endDate) {
    }

    public record UnassignedItem(Long taskId, String taskName, List<String> skills, String reason) {
    }

    public record AutoAssignResponse(boolean dryRun, LocalDate startDate, List<AutoAssignedItem> assigned,
                                     List<UnassignedItem> unassigned) {
    }
}
