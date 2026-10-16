package com.jutinico.scheduler.assignment;

import java.time.LocalDate;
import java.util.List;

import com.jutinico.scheduler.common.Limits;
import com.jutinico.scheduler.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    public record AssignRequest(
            @NotNull Long workerId,
            @NotEmpty @Size(max = Limits.TASKS_PER_ASSIGNMENT_MAX) @UniqueElements List<@NotNull Long> taskIds,
            @NotNull @FutureOrPresent LocalDate startDate) {
    }

    public record ReassignRequest(
            @NotNull Long workerId,
            @NotNull @FutureOrPresent LocalDate startDate) {
    }

    /** {@code startDate} es hoy por defecto; {@code dryRun} solo muestra una vista previa. */
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
