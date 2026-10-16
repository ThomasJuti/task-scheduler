package com.jutinico.scheduler.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityApiTest extends ApiIntegrationTest {

    @Test
    void requestsWithoutValidTokenAreUnauthorized() throws Exception {
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCredentialsGetAGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void repeatedFailuresLockTheAccountTemporarily() throws Exception {
        String bad = "{\"username\":\"locked-user\",\"password\":\"wrong\"}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void userCanReadAndMoveTasksButNotEditThem() throws Exception {
        long task = createTask("API", 4, "[\"Java\"]");
        long worker = createWorker("Ana", 8, "[\"Java\"]");

        mvc.perform(as(userToken, get("/api/tasks"))).andExpect(status().isOk());
        mvc.perform(as(userToken, get("/api/workers"))).andExpect(status().isOk());
        mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(worker, task, NEXT_MONDAY)))
                .andExpect(status().isCreated());

        // status endpoint does not accept other fields
        mvc.perform(json(userToken, patch("/api/tasks/" + task + "/status"), "{\"status\":\"DONE\",\"name\":\"hack\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(userToken, post("/api/tasks"),
                        "{\"name\":\"X\",\"effortHours\":1,\"skills\":[\"Java\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(userToken, put("/api/tasks/" + task),
                        "{\"name\":\"X\",\"effortHours\":1,\"skills\":[\"Java\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(userToken, delete("/api/tasks/" + task))).andExpect(status().isForbidden());
        mvc.perform(json(userToken, post("/api/workers"),
                        "{\"name\":\"X\",\"availabilityHoursPerDay\":8,\"skills\":[\"Java\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(userToken, post("/api/assignments/auto"), "{}")).andExpect(status().isForbidden());
        mvc.perform(as(userToken, delete("/api/admin/reset"))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanResetData() throws Exception {
        createTask("API", 4, "[\"Java\"]");
        mvc.perform(as(adminToken, delete("/api/admin/reset"))).andExpect(status().isNoContent());
        mvc.perform(as(adminToken, get("/api/tasks"))).andExpect(jsonPath("$").isEmpty());
    }
}
