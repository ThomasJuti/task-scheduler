package com.jutinico.scheduler.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class CrudApiTest extends ApiIntegrationTest {

    @Test
    void taskCrudNormalizesNamesAndSkills() throws Exception {
        long id = createTask("  Build   login ", 6, "[\"Java\", \"java\", \" Spring  Boot \"]");

        mvc.perform(as(adminToken, get("/api/tasks/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Build login"))
                .andExpect(jsonPath("$.skills", contains("Java", "Spring Boot")))
                .andExpect(jsonPath("$.status").value("NOT_DONE"))
                .andExpect(jsonPath("$.assignment").doesNotExist());

        mvc.perform(json(adminToken, put("/api/tasks/" + id),
                        "{\"name\":\"Build login\",\"effortHours\":8,\"skills\":[\"Java\"],\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effortHours").value(8))
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(as(adminToken, get("/api/tasks?status=DONE"))).andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(as(adminToken, get("/api/skills"))).andExpect(jsonPath("$", contains("Java", "Spring Boot")));

        mvc.perform(as(adminToken, delete("/api/tasks/" + id))).andExpect(status().isNoContent());
        mvc.perform(as(adminToken, get("/api/tasks/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task " + id + " not found"));
    }

    @Test
    void workerCrud() throws Exception {
        long id = createWorker("Ana", 8, "[\"Java\", \"SQL\"]");

        mvc.perform(json(adminToken, put("/api/workers/" + id),
                        "{\"name\":\"Ana Gómez\",\"availabilityHoursPerDay\":6,\"skills\":[\"Java\",\"SQL\",\"Angular\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Gómez"))
                .andExpect(jsonPath("$.availabilityHoursPerDay").value(6))
                .andExpect(jsonPath("$.skills", contains("Angular", "Java", "SQL")));

        mvc.perform(as(adminToken, get("/api/workers"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(adminToken, delete("/api/workers/" + id))).andExpect(status().isNoContent());
        mvc.perform(as(adminToken, get("/api/workers"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void skillsAreSharedAndDeduplicatedCaseInsensitively() throws Exception {
        long worker = createWorker("Ana", 8, "[\"Java\", \"JAVA\", \" java \"]");
        createTask("API", 4, "[\"java\", \"Sql\"]");

        mvc.perform(as(adminToken, get("/api/workers/" + worker))).andExpect(jsonPath("$.skills", contains("Java")));
        // la tarea reutiliza el skill "Java" existente en vez de crear "java"
        mvc.perform(as(adminToken, get("/api/skills"))).andExpect(jsonPath("$", contains("Java", "Sql")));
    }

    @Test
    void changingTheSkillsOfAnAssignedTaskIsRejected() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\", \"SQL\"]");
        long task = createTask("API", 8, "[\"Java\"]");
        assign(ana, task);

        mvc.perform(json(adminToken, put("/api/tasks/" + task),
                        "{\"name\":\"API\",\"effortHours\":8,\"skills\":[\"Java\",\"SQL\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("unassign it before")));
        // el estado sí puede cambiar, no afecta la planificación
        mvc.perform(json(adminToken, put("/api/tasks/" + task),
                        "{\"name\":\"API\",\"effortHours\":8,\"skills\":[\"JAVA\"],\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void unassignedFilterSplitsAssignedAndPendingTasks() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long assigned = createTask("Assigned", 4, "[\"Java\"]");
        long pending = createTask("Pending", 4, "[\"Java\"]");
        assign(ana, assigned);

        mvc.perform(as(adminToken, get("/api/tasks?unassigned=true")))
                .andExpect(jsonPath("$.content[*].id", contains((int) pending)));
        mvc.perform(as(adminToken, get("/api/tasks?unassigned=false")))
                .andExpect(jsonPath("$.content[*].id", contains((int) assigned)));
        mvc.perform(as(adminToken, get("/api/tasks"))).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void deletingAWorkerRemovesItsAssignmentsButKeepsTheTasks() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 8, "[\"Java\"]");
        assign(ana, task);

        mvc.perform(as(adminToken, delete("/api/workers/" + ana))).andExpect(status().isNoContent());

        mvc.perform(as(adminToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(as(adminToken, get("/api/tasks/" + task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment").doesNotExist());
    }

    @Test
    void deletingAnAssignedTaskRemovesItsAssignment() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 8, "[\"Java\"]");
        assign(ana, task);

        mvc.perform(as(adminToken, delete("/api/tasks/" + task))).andExpect(status().isNoContent());

        mvc.perform(as(adminToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(as(adminToken, get("/api/workers/" + ana))).andExpect(jsonPath("$.assignedHours").value(0));
    }

    private void assign(long workerId, long taskId) throws Exception {
        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(workerId, taskId, NEXT_MONDAY)))
                .andExpect(status().isCreated());
    }
}
