package com.jutinico.scheduler.task;

import java.time.LocalDate;
import java.util.List;

import com.jutinico.scheduler.common.Limits;
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
            @NotBlank @Size(max = Limits.TASK_NAME_MAX) String name,
            @NotNull @Min(Limits.EFFORT_MIN) @Max(Limits.EFFORT_MAX) Integer effortHours,
            @NotEmpty @Size(max = Limits.TASK_SKILLS_MAX) List<
                    @NotBlank @Size(max = Limits.SKILL_NAME_MAX) @Pattern(regexp = ValidationPatterns.SKILL, message = ValidationPatterns.SKILL_MESSAGE)
                    String> skills,
            @NotNull TaskStatus status) {
    }

    public record TaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record AssignmentSummary(Long id, Long workerId, String workerName, LocalDate startDate, LocalDate endDate) {
    }

    public record TaskResponse(Long id, String name, int effortHours, List<String> skills, TaskStatus status,
                               AssignmentSummary assignment) {
    }
}
