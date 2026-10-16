package com.jutinico.scheduler.worker;

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

public final class WorkerDtos {

    private WorkerDtos() {
    }

    public record WorkerRequest(
            @NotBlank @Size(max = Limits.WORKER_NAME_MAX)
            @Pattern(regexp = ValidationPatterns.NO_CONTROL_CHARS, message = ValidationPatterns.NO_CONTROL_CHARS_MESSAGE) String name,
            @NotNull @Min(Limits.AVAILABILITY_MIN) @Max(Limits.AVAILABILITY_MAX) Integer availabilityHoursPerDay,
            @NotEmpty @Size(max = Limits.WORKER_SKILLS_MAX) List<
                    @NotBlank @Size(max = Limits.SKILL_NAME_MAX) @Pattern(regexp = ValidationPatterns.SKILL, message = ValidationPatterns.SKILL_MESSAGE)
                    String> skills) {
    }

    public record WorkerResponse(Long id, String name, int availabilityHoursPerDay, List<String> skills,
                                 int assignedTasks, int assignedHours) {
    }
}
