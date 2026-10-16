package com.jutinico.scheduler.worker;

import java.util.List;

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
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(1) @Max(24) Integer availabilityHoursPerDay,
            @NotEmpty @Size(max = 30) List<
                    @NotBlank @Size(max = 50) @Pattern(regexp = ValidationPatterns.SKILL, message = ValidationPatterns.SKILL_MESSAGE)
                    String> skills) {
    }

    public record WorkerResponse(Long id, String name, int availabilityHoursPerDay, List<String> skills,
                                 int assignedTasks, int assignedHours) {
    }
}
