package com.jutinico.scheduler.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mvc.perform(as(adminToken, get("/api/tasks?status=DONE"))).andExpect(jsonPath("$", hasSize(1)));
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
}
