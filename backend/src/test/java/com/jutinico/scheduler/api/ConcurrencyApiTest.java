package com.jutinico.scheduler.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jutinico.scheduler.assignment.AssignmentDtos.AssignRequest;
import com.jutinico.scheduler.assignment.AssignmentService;
import com.jutinico.scheduler.skill.SkillService;
import com.jutinico.scheduler.task.TaskService;
import com.jutinico.scheduler.task.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carreras entre editar y asignar (COR-001) y entre crear el mismo skill (COR-002). Una transacción queda abierta
 * a mitad de una operación; la segunda petición debe esperar a que termine y responder según el estado final,
 * en vez de leer datos viejos y romper Req3a o la coherencia esfuerzo/horas.
 */
class ConcurrencyApiTest extends ApiIntegrationTest {

    private static final long WAIT_MS = 700;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    TaskService taskService;

    @Autowired
    SkillService skillService;

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    @Test
    void removingASkillWaitsForAnAssignmentInProgressAndIsThenRejected() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\", \"SQL\"]");
        long task = createTask("API", 8, "[\"Java\"]");

        int status = whileUncommitted(
                () -> assignmentService.assign(new AssignRequest(ana, List.of(task), NEXT_MONDAY)),
                () -> mvc.perform(json(adminToken, put("/api/workers/" + ana),
                        "{\"name\":\"Ana\",\"availabilityHoursPerDay\":8,\"skills\":[\"SQL\"]}")).andReturn().getResponse().getStatus());

        assertThat(status).as("Ana ya tiene asignada una tarea que necesita Java").isEqualTo(409);
    }

    @Test
    void changingTheEffortWaitsForAnAssignmentInProgressAndIsThenRejected() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 8, "[\"Java\"]");

        int status = whileUncommitted(
                () -> assignmentService.assign(new AssignRequest(ana, List.of(task), NEXT_MONDAY)),
                () -> mvc.perform(json(adminToken, put("/api/tasks/" + task),
                        "{\"name\":\"API\",\"effortHours\":40,\"skills\":[\"Java\"],\"status\":\"NOT_DONE\"}")).andReturn().getResponse().getStatus());

        assertThat(status).as("las horas planificadas ya no coincidirían con el esfuerzo").isEqualTo(409);
    }

    @Test
    void assigningWaitsForAStatusChangeInProgressAndThenSeesTheTaskDone() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 8, "[\"Java\"]");

        int status = whileUncommitted(
                () -> taskService.updateStatus(task, TaskStatus.DONE),
                () -> mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY)))
                        .andReturn().getResponse().getStatus());

        assertThat(status).as("no se puede asignar una tarea que quedó DONE").isEqualTo(409);
    }

    @Test
    void creatingTheSameNewSkillConcurrentlyDoesNotFail() throws Exception {
        int status = whileUncommitted(
                () -> skillService.resolve(List.of("Rust")),
                () -> mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"CLI\",\"effortHours\":4,\"skills\":[\"rust\"],\"status\":\"NOT_DONE\"}")).andReturn().getResponse().getStatus());

        assertThat(status).as("la segunda petición reutiliza el skill en vez de chocar con el índice único").isEqualTo(201);
    }

    @Test
    void movingATaskStatusWaitsForTheSameLockAsEditingIt() throws Exception {
        long task = createTask("API", 8, "[\"Java\"]");

        int status = whileUncommitted(
                () -> taskService.updateStatus(task, TaskStatus.DONE),
                () -> mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"NOT_DONE\"}"))
                        .andReturn().getResponse().getStatus());

        assertThat(status).isEqualTo(200);
    }

    /**
     * Ejecuta {@code inProgress} en una transacción que queda sin confirmar, lanza {@code request}, comprueba que
     * espera (bloqueada por la fila) y solo entonces confirma la primera. Devuelve el código HTTP de la segunda.
     */
    private int whileUncommitted(Runnable inProgress, Callable<Integer> request) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        Future<?> holder = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            inProgress.run();
            started.countDown();
            await(commit);
        }));
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();

        Future<Integer> second = pool.submit(request);
        Thread.sleep(WAIT_MS);
        assertThat(second.isDone()).as("la segunda petición debe esperar a la transacción en curso").isFalse();

        commit.countDown();
        holder.get(10, TimeUnit.SECONDS);
        return second.get(10, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
