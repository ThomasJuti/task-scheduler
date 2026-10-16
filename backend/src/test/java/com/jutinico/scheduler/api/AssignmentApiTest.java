package com.jutinico.scheduler.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class AssignmentApiTest extends ApiIntegrationTest {

    @Test
    void manualAssignmentSchedulesTasksBackToBack() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\",\"SQL\"]");
        long api = createTask("API", 12, "[\"Java\"]");
        long db = createTask("DB", 6, "[\"SQL\"]");

        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d,%d],\"startDate\":\"%s\"}".formatted(ana, api, db, NEXT_MONDAY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                // API: lun 8 + mar 4; BD: mar 4 + mié 2
                .andExpect(jsonPath("$[0].startDate").value(NEXT_MONDAY.toString()))
                .andExpect(jsonPath("$[0].endDate").value(NEXT_MONDAY.plusDays(1).toString()))
                .andExpect(jsonPath("$[1].startDate").value(NEXT_MONDAY.plusDays(1).toString()))
                .andExpect(jsonPath("$[1].endDate").value(NEXT_MONDAY.plusDays(2).toString()))
                .andExpect(jsonPath("$[1].allocations", hasSize(2)));

        mvc.perform(as(userToken, get("/api/tasks/" + api)))
                .andExpect(jsonPath("$.assignment.workerName").value("Ana"));
        mvc.perform(as(userToken, get("/api/workers/" + ana)))
                .andExpect(jsonPath("$.assignedTasks").value(2))
                .andExpect(jsonPath("$.assignedHours").value(18));
    }

    @Test
    void req3aRejectsTasksWhoseSkillsAreNotASubsetOfTheWorkerSkills() throws Exception {
        long designer = createWorker("Dani", 8, "[\"Figma\"]");
        long ok = createTask("Mockups", 4, "[\"figma\"]");
        long backend = createTask("API", 4, "[\"Java\",\"Figma\"]");

        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d,%d],\"startDate\":\"%s\"}".formatted(designer, ok, backend, NEXT_MONDAY)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("missing skills [Java]")));

        // todo o nada: la tarea compatible tampoco se asignó
        mvc.perform(as(userToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void doneOrAlreadyAssignedTasksCannotBeAssigned() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 4, "[\"Java\"]");
        String body = "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY);

        mvc.perform(json(userToken, post("/api/assignments"), body)).andExpect(status().isCreated());
        mvc.perform(json(userToken, post("/api/assignments"), body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already assigned")));

        long done = createTask("Old", 2, "[\"Java\"]");
        mvc.perform(json(userToken, patch("/api/tasks/" + done + "/status"), "{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, done, NEXT_MONDAY)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already done")));
    }

    @Test
    void reassignMovesTheTaskAndRechecksSkills() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long bob = createWorker("Bob", 4, "[\"Java\"]");
        long dani = createWorker("Dani", 8, "[\"Figma\"]");
        long task = createTask("API", 8, "[\"Java\"]");
        String created = mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY)))
                .andReturn().getResponse().getContentAsString();
        long assignmentId = ((Number) JsonPath.read(created, "$[0].id")).longValue();

        mvc.perform(json(userToken, patch("/api/assignments/" + assignmentId),
                        "{\"workerId\":%d,\"startDate\":\"%s\"}".formatted(bob, NEXT_MONDAY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerName").value("Bob"))
                .andExpect(jsonPath("$.endDate").value(NEXT_MONDAY.plusDays(1).toString()));

        mvc.perform(json(userToken, patch("/api/assignments/" + assignmentId),
                        "{\"workerId\":%d,\"startDate\":\"%s\"}".formatted(dani, NEXT_MONDAY)))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(as(userToken, delete("/api/assignments/" + assignmentId))).andExpect(status().isNoContent());
        mvc.perform(as(userToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void assignedTaskEffortAndWorkerSkillsAreProtected() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\",\"SQL\"]");
        long task = createTask("API", 8, "[\"Java\"]");
        mvc.perform(json(userToken, post("/api/assignments"),
                "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY)));

        mvc.perform(json(adminToken, put("/api/tasks/" + task),
                        "{\"name\":\"API v2\",\"effortHours\":20,\"skills\":[\"Java\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(json(adminToken, put("/api/workers/" + ana),
                        "{\"name\":\"Ana\",\"availabilityHoursPerDay\":8,\"skills\":[\"SQL\"]}"))
                .andExpect(status().isConflict());
        // renombrar está permitido
        mvc.perform(json(adminToken, put("/api/tasks/" + task),
                        "{\"name\":\"API v2\",\"effortHours\":8,\"skills\":[\"java\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void autoAssignPreviewThenRun() throws Exception {
        createWorker("Ana", 8, "[\"Java\"]");
        createWorker("Bob", 8, "[\"Java\",\"SQL\"]");
        createTask("Big", 16, "[\"Java\"]");
        createTask("Report", 4, "[\"SQL\"]");
        createTask("Infra", 4, "[\"Kubernetes\"]");
        String body = "{\"startDate\":\"%s\",\"dryRun\":%s}";

        mvc.perform(json(adminToken, post("/api/assignments/auto"), body.formatted(NEXT_MONDAY, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.assigned", hasSize(2)))
                .andExpect(jsonPath("$.unassigned[0].taskName").value("Infra"));
        mvc.perform(as(adminToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(json(adminToken, post("/api/assignments/auto"), body.formatted(NEXT_MONDAY, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned[0].workerName").value("Ana"))
                .andExpect(jsonPath("$.assigned[1].workerName").value("Bob"));
        mvc.perform(as(adminToken, get("/api/assignments"))).andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(as(adminToken, get("/api/tasks?unassigned=true"))).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void doneTasksKeepTheirAssignmentUntilMarkedNotDone() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 4, "[\"Java\"]");
        String created = mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY)))
                .andReturn().getResponse().getContentAsString();
        long assignmentId = ((Number) JsonPath.read(created, "$[0].id")).longValue();
        mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"DONE\"}"));

        mvc.perform(as(userToken, delete("/api/assignments/" + assignmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("mark it as not done")));

        mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"NOT_DONE\"}"));
        mvc.perform(as(userToken, delete("/api/assignments/" + assignmentId))).andExpect(status().isNoContent());
    }
}
