package com.jutinico.scheduler.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignResponse;
import com.jutinico.scheduler.assignment.AssignmentDtos.AutoAssignedItem;
import com.jutinico.scheduler.skill.Skill;
import com.jutinico.scheduler.task.Task;
import com.jutinico.scheduler.task.TaskRepository;
import com.jutinico.scheduler.task.TaskStatus;
import com.jutinico.scheduler.worker.Worker;
import com.jutinico.scheduler.worker.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AutoAssignmentServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    private final AtomicLong ids = new AtomicLong();
    private AssignmentRepository assignments;
    private WorkerRepository workers;
    private TaskRepository tasks;
    private AutoAssignmentService service;

    @BeforeEach
    void setUp() {
        assignments = mock(AssignmentRepository.class);
        workers = mock(WorkerRepository.class);
        tasks = mock(TaskRepository.class);
        when(assignments.findAllWithWorker()).thenReturn(List.of());
        Clock clock = Clock.fixed(MONDAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new AutoAssignmentService(assignments, workers, tasks, clock);
    }

    @Test
    void picksTheCompatibleWorkerThatFinishesFirst() {
        Worker slow = worker("Slow", 2, "java");
        Worker fast = worker("Fast", 8, "java", "sql");
        Worker noJava = worker("Designer", 8, "figma");
        Task task = task("API", 16, "java");
        given(List.of(slow, fast, noJava), List.of(task));

        AutoAssignResponse result = service.run(MONDAY, false);

        assertThat(result.assigned()).singleElement().satisfies(item -> {
            assertThat(item.workerName()).isEqualTo("Fast");
            assertThat(item.startDate()).isEqualTo(MONDAY);
            assertThat(item.endDate()).isEqualTo(MONDAY.plusDays(1));
        });
        assertThat(result.unassigned()).isEmpty();
        verify(assignments, times(1)).save(any(Assignment.class));
    }

    @Test
    void balancesLoadUsingCapacityAlreadyTakenInTheSameRun() {
        Worker ana = worker("Ana", 8, "java");
        Worker bob = worker("Bob", 8, "java");
        Task big = task("Big", 16, "java");
        Task medium = task("Medium", 8, "java");
        Task small = task("Small", 4, "java");
        given(List.of(ana, bob), List.of(small, big, medium));

        AutoAssignResponse result = service.run(MONDAY, false);

        // Biggest first: Big -> Ana (Mon-Tue), Medium -> Bob (Mon), Small -> Bob (Tue) finishes before Ana's Wed
        assertThat(result.assigned()).extracting(AutoAssignedItem::taskName, AutoAssignedItem::workerName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Big", "Ana"),
                        org.assertj.core.groups.Tuple.tuple("Medium", "Bob"),
                        org.assertj.core.groups.Tuple.tuple("Small", "Bob"));
        assertThat(result.assigned().get(2).endDate()).isEqualTo(MONDAY.plusDays(1));
    }

    @Test
    void reportsTasksWithoutCompatibleWorker() {
        Worker dev = worker("Dev", 8, "java");
        Task devops = task("Pipeline", 5, "java", "kubernetes");
        given(List.of(dev), List.of(devops));

        AutoAssignResponse result = service.run(MONDAY, false);

        assertThat(result.assigned()).isEmpty();
        assertThat(result.unassigned()).singleElement().satisfies(item -> {
            assertThat(item.taskName()).isEqualTo("Pipeline");
            assertThat(item.reason()).contains("kubernetes");
        });
    }

    @Test
    void dryRunDoesNotSaveAnything() {
        given(List.of(worker("Dev", 8, "java")), List.of(task("API", 4, "java")));

        AutoAssignResponse result = service.run(MONDAY, true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.assigned()).hasSize(1);
        verify(assignments, never()).save(any());
    }

    @Test
    void defaultsToTodayAndSkipsWeekends() {
        given(List.of(worker("Dev", 8, "java")), List.of(task("API", 4, "java")));

        assertThat(service.run(null, true).startDate()).isEqualTo(MONDAY);
        assertThat(service.run(MONDAY.minusDays(2), true).startDate()).isEqualTo(MONDAY);
    }

    private void given(List<Worker> allWorkers, List<Task> pending) {
        when(workers.findAllByOrderByIdAsc()).thenReturn(allWorkers);
        when(tasks.findPendingUnassigned()).thenReturn(pending);
    }

    private Worker worker(String name, int availability, String... skills) {
        Worker worker = new Worker(name, availability, skills(skills));
        ReflectionTestUtils.setField(worker, "id", ids.incrementAndGet());
        return worker;
    }

    private Task task(String name, int effort, String... skills) {
        Task task = new Task(name, effort, TaskStatus.NOT_DONE, skills(skills));
        ReflectionTestUtils.setField(task, "id", ids.incrementAndGet());
        return task;
    }

    private static Set<Skill> skills(String... names) {
        return Arrays.stream(names).map(Skill::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
