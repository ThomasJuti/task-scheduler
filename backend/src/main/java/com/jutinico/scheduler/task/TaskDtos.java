package com.jutinico.scheduler.task;

import java.time.LocalDate;
import java.util.List;

import com.jutinico.scheduler.common.ValidationPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(1) @Max(1000) Integer effortHours,
            @NotEmpty @Size(max = 20) List<
                    @NotBlank @Size(max = 50) @Pattern(regexp = ValidationPatterns.SKILL, message = ValidationPatterns.SKILL_MESSAGE)
                    String> skills,
            @NotNull TaskStatus status) {
    }

    /** The only change a USER may make to a task ("moving" it between done / not done). */
    public record TaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record AssignmentSummary(Long id, Long workerId, String workerName, LocalDate startDate, LocalDate endDate) {
    }

    public record TaskResponse(Long id, String name, int effortHours, List<String> skills, TaskStatus status,
                               AssignmentSummary assignment) {
    }
}
