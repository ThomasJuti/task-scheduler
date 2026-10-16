package com.jutinico.scheduler.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

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
}
