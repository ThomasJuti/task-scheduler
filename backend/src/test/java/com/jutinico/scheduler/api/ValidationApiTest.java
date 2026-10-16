package com.jutinico.scheduler.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ValidationApiTest extends ApiIntegrationTest {

    @Test
    void invalidTaskFieldsAreReportedPerField() throws Exception {
        mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"  \",\"effortHours\":0,\"skills\":[],\"status\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.effortHours").exists())
                .andExpect(jsonPath("$.errors.skills").exists())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void skillNamesMustBeSafe() throws Exception {
        mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"X\",\"effortHours\":1,\"skills\":[\"<script>\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['skills[0]']").exists());
    }

    @Test
    void workerAvailabilityMustBeBetween1And24() throws Exception {
        mvc.perform(json(adminToken, post("/api/workers"),
                        "{\"name\":\"Ana\",\"availabilityHoursPerDay\":25,\"skills\":[\"Java\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.availabilityHoursPerDay").exists());
    }

    @Test
    void unknownFieldsWrongTypesAndBadJsonAreRejected() throws Exception {
        mvc.perform(json(adminToken, post("/api/workers"),
                        "{\"name\":\"Ana\",\"availabilityHoursPerDay\":8,\"skills\":[\"Java\"],\"salary\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(adminToken, post("/api/workers"),
                        "{\"name\":\"Ana\",\"availabilityHoursPerDay\":\"eight\",\"skills\":[\"Java\"]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"X\",\"effortHours\":1,\"skills\":[\"Java\"],\"status\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(adminToken, post("/api/tasks"), "{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentStartDateCannotBeInThePastAndTaskIdsMustBeUnique() throws Exception {
        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":1,\"taskIds\":[1,1],\"startDate\":\"2020-01-06\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").exists())
                .andExpect(jsonPath("$.errors.taskIds").exists());
    }

    @Test
    void startDatesBeyondThePlanningHorizonAreRejected() throws Exception {
        long ana = createWorker("Ana", 8, "[\"Java\"]");
        long task = createTask("API", 4, "[\"Java\"]");

        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"+10003-01-01\"}".formatted(ana, task)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").exists());
        // Antes daba 500: la fecha máxima de Java desbordaba el calendario
        mvc.perform(json(adminToken, post("/api/assignments/auto"), "{\"startDate\":\"+999999999-12-31\",\"dryRun\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").exists());
        mvc.perform(json(userToken, post("/api/assignments"),
                        "{\"workerId\":%d,\"taskIds\":[%d],\"startDate\":\"%s\"}".formatted(ana, task, NEXT_MONDAY.plusYears(1))))
                .andExpect(status().isCreated());
    }

    @Test
    void controlCharactersInNamesAreAValidationError() throws Exception {
        mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"a\\u0000b\",\"effortHours\":1,\"skills\":[\"Java\"],\"status\":\"NOT_DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("must not contain control characters"));
        mvc.perform(json(adminToken, post("/api/workers"),
                        "{\"name\":\"Ana\\nB\",\"availabilityHoursPerDay\":8,\"skills\":[\"Java\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ad\\u0000min\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
