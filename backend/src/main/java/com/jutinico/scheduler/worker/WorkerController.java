package com.jutinico.scheduler.worker;

import java.net.URI;
import java.util.List;

import com.jutinico.scheduler.worker.WorkerDtos.WorkerRequest;
import com.jutinico.scheduler.worker.WorkerDtos.WorkerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping
    public List<WorkerResponse> list() {
        return workerService.list();
    }

    @GetMapping("/{id}")
    public WorkerResponse get(@PathVariable Long id) {
        return workerService.get(id);
    }

    @PostMapping
    public ResponseEntity<WorkerResponse> create(@Valid @RequestBody WorkerRequest request) {
        WorkerResponse created = workerService.create(request);
        return ResponseEntity.created(URI.create("/api/workers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public WorkerResponse update(@PathVariable Long id, @Valid @RequestBody WorkerRequest request) {
        return workerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
