package com.jutinico.scheduler.assignment;

import java.util.List;

import com.jutinico.scheduler.assignment.AssignmentDtos.AssignRequest;
import com.jutinico.scheduler.assignment.AssignmentDtos.AssignmentResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignRequest;
import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.ReassignRequest;
import com.jutinico.scheduler.common.Limits;
import com.jutinico.scheduler.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AutoAssignmentService autoAssignmentService;

    public AssignmentController(AssignmentService assignmentService, AutoAssignmentService autoAssignmentService) {
        this.assignmentService = assignmentService;
        this.autoAssignmentService = autoAssignmentService;
    }

    /** Paginado: {@code ?page=0&size=20&workerId=1}. */
    @GetMapping
    public PageResponse<AssignmentResponse> list(
            @RequestParam(required = false) Long workerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + Limits.PAGE_SIZE_DEFAULT) @Min(1) @Max(Limits.PAGE_SIZE_MAX) int size) {
        return assignmentService.list(workerId, page, size);
    }

    @PostMapping
    public ResponseEntity<List<AssignmentResponse>> assign(@Valid @RequestBody AssignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assign(request));
    }

    @PatchMapping("/{id}")
    public AssignmentResponse reassign(@PathVariable Long id, @Valid @RequestBody ReassignRequest request) {
        return assignmentService.reassign(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unassign(@PathVariable Long id) {
        assignmentService.unassign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto")
    public AutoAssignResponse autoAssign(@Valid @RequestBody(required = false) AutoAssignRequest request) {
        AutoAssignRequest body = request != null ? request : new AutoAssignRequest(null, false);
        return autoAssignmentService.run(body.startDate(), Boolean.TRUE.equals(body.dryRun()));
    }
}
