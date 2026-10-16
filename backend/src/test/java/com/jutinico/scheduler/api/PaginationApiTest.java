package com.jutinico.scheduler.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** PERF-001: los listados se paginan, filtran y ordenan en la base de datos, y la carga no usa el historial completo. */
class PaginationApiTest extends ApiIntegrationTest {

    @Test
    void tasksArePagedAndSortedOnTheServer() throws Exception {
        for (int i = 1; i <= 25; i++) {
            createTask("Task " + i, i, "[\"Java\"]");
        }

        mvc.perform(as(adminToken, get("/api/tasks")))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(25));
        mvc.perform(as(adminToken, get("/api/tasks?page=1&size=10&sort=effortHours,desc")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].effortHours").value(15))
                .andExpect(jsonPath("$.content[9].effortHours").value(6));
    }

    @Test
    void namesSortIgnoringCase() throws Exception {
        createTask("beta", 1, "[\"Java\"]");
        createTask("Alpha", 1, "[\"Java\"]");
        createTask("charlie", 1, "[\"Java\"]");

        mvc.perform(as(adminToken, get("/api/tasks?sort=name")))
                .andExpect(jsonPath("$.content[*].name", contains("Alpha", "beta", "charlie")));
    }

    @Test
    void searchMatchesNameSkillOrAssignedWorker() throws Exception {
        long bruno = createWorker("Bruno", 8, "[\"Java\", \"Angular\"]");
        createTask("Login page", 4, "[\"Angular\"]");
        long api = createTask("REST API", 4, "[\"Java\"]");
        mvc.perform(json(userToken, post("/api/assignments"),
                "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(bruno, api, NEXT_MONDAY)));

        mvc.perform(as(userToken, get("/api/tasks?q=LOGIN"))).andExpect(jsonPath("$.content[*].name", contains("Login page")));
        mvc.perform(as(userToken, get("/api/tasks?q=java"))).andExpect(jsonPath("$.content[*].name", contains("REST API")));
        mvc.perform(as(userToken, get("/api/tasks?q=bruno"))).andExpect(jsonPath("$.content[*].name", contains("REST API")));
        // los comodines de LIKE se buscan literalmente
        mvc.perform(as(userToken, get("/api/tasks?q=%25"))).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidPagingParametersAreRejected() throws Exception {
        mvc.perform(as(userToken, get("/api/tasks?size=101"))).andExpect(status().isBadRequest());
        mvc.perform(as(userToken, get("/api/tasks?page=-1"))).andExpect(status().isBadRequest());
        mvc.perform(as(userToken, get("/api/tasks?sort=password"))).andExpect(status().isBadRequest());
        mvc.perform(as(userToken, get("/api/tasks?sort=name,sideways"))).andExpect(status().isBadRequest());
        mvc.perform(as(userToken, get("/api/assignments?size=0"))).andExpect(status().isBadRequest());
    }

    @Test
    void statsCountWithoutLoadingTheTasks() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long assigned = createTask("Assigned", 4, "[\"Java\"]");
        createTask("Pending", 4, "[\"Java\"]");
        long done = createTask("Done", 4, "[\"Java\"]");
        mvc.perform(json(adminToken, post("/api/assignments"),
                "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, assigned, NEXT_MONDAY)));
        mvc.perform(json(adminToken, patch("/api/tasks/" + done + "/status"), "{\"status\":\"DONE\"}"));

        mvc.perform(as(userToken, get("/api/tasks/stats")))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.done").value(1))
                .andExpect(jsonPath("$.pendingUnassigned").value(1));
    }

    @Test
    void assignmentsArePagedAndFilteredByWorker() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\", \"SQL\"]");
        long bob = createWorker("Bob", 8, "[\"Java\"]");
        long t1 = createTask("T1", 4, "[\"Java\"]");
        long t2 = createTask("T2", 4, "[\"Java\"]");
        long t3 = createTask("T3", 4, "[\"Java\", \"SQL\"]");
        long t4 = createTask("T4", 4, "[\"Java\"]");
        mvc.perform(json(userToken, post("/api/assignments"),
                "{\"workerId\":%d,\"taskIds\":[%d,%d,%d],\"startDate\":\"%s\"}".formatted(ana, t1, t2, t3, NEXT_MONDAY)));
        mvc.perform(json(userToken, post("/api/assignments"),
                "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(bob, t4, NEXT_MONDAY)));

        mvc.perform(as(userToken, get("/api/assignments?size=2")))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(as(userToken, get("/api/assignments?workerId=" + bob)))
                .andExpect(jsonPath("$.content[*].taskName", contains("T4")));
        mvc.perform(as(userToken, get("/api/assignments?workerId=" + ana)))
                .andExpect(jsonPath("$.content[2].taskSkills", contains("Java", "SQL")));
        // la carga de la lista de trabajadores sale de un GROUP BY
        mvc.perform(as(userToken, get("/api/workers")))
                .andExpect(jsonPath("$[0].assignedTasks").value(3))
                .andExpect(jsonPath("$[0].assignedHours").value(12))
                .andExpect(jsonPath("$[1].assignedTasks").value(1));
    }

    @Test
    void finishedAssignmentsDoNotCountAsCurrentWorkload() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        createWorker("Bob", 8, "[\"Java\"]");
        long old = createTask("Old work", 40, "[\"Java\"]");
        createTask("New work", 8, "[\"Java\"]");
        // Historial: Ana tiene una asignación que terminó hace un mes (insertada directo: la API no acepta fechas pasadas)
        LocalDate start = NEXT_MONDAY.minusWeeks(5);
        jdbc.update("update task set status = 'DONE' where id = ?", old);
        jdbc.update("insert into assignment (worker_id, task_id, start_date, end_date) values (?, ?, ?, ?)",
                ana, old, start, start.plusDays(4));
        jdbc.update("insert into assignment_allocation (assignment_id, work_date, hours) "
                + "select id, ?, 8 from assignment where task_id = ?", start, old);

        // Ana y Bob terminan el mismo día; si contara el historial, Ana tendría más horas y perdería el desempate
        mvc.perform(json(adminToken, post("/api/assignments/auto"), "{\"startDate\":\"%s\",\"dryRun\":true}".formatted(NEXT_MONDAY)))
                .andExpect(jsonPath("$.assigned[0].workerName").value("Ana"));
    }
}
