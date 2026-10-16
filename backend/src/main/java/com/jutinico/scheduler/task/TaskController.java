package com.jutinico.scheduler.task;

import java.net.URI;

import com.jutinico.scheduler.common.Limits;
import com.jutinico.scheduler.common.PageResponse;
import com.jutinico.scheduler.common.ValidationPatterns;
import com.jutinico.scheduler.task.TaskDtos.TaskRequest;
import com.jutinico.scheduler.task.TaskDtos.TaskResponse;
import com.jutinico.scheduler.task.TaskDtos.TaskStats;
import com.jutinico.scheduler.task.TaskDtos.TaskStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final String SORT = "^(id|name|effortHours|status)(,(asc|desc))?$";

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** Paginado: {@code ?page=0&size=20&sort=name,asc&q=texto&status=NOT_DONE&unassigned=true}. */
    @GetMapping
    public PageResponse<TaskResponse> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) @Size(max = Limits.SEARCH_MAX)
            @Pattern(regexp = ValidationPatterns.NO_CONTROL_CHARS, message = ValidationPatterns.NO_CONTROL_CHARS_MESSAGE) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + Limits.PAGE_SIZE_DEFAULT) @Min(1) @Max(Limits.PAGE_SIZE_MAX) int size,
            @RequestParam(defaultValue = "id") @Pattern(regexp = SORT, message = "must be one of id, name, effortHours, status, optionally followed by ,asc or ,desc") String sort) {
        return taskService.list(status, unassigned, q, page, size, sort);
    }

    @GetMapping("/stats")
    public TaskStats stats() {
        return taskService.stats();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
