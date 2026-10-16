package com.jutinico.scheduler.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.jayway.jsonpath.JsonPath;
import com.jutinico.scheduler.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Aplicación completa contra un PostgreSQL real (Testcontainers), llamada con MockMvc y JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
abstract class ApiIntegrationTest {

    /** Un lunes futuro, para que las fechas de inicio con @FutureOrPresent siempre sean válidas. */
    static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    String adminToken;
    String userToken;

    @BeforeEach
    void cleanDatabaseAndLogin() throws Exception {
        jdbc.execute("TRUNCATE assignment_allocation, assignment, task_skill, worker_skill, task, worker, skill RESTART IDENTITY CASCADE");
        adminToken = login("admin", "admin-test-pass");
        userToken = login("user", "user-test-pass");
    }

    String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    static MockHttpServletRequestBuilder json(String token, MockHttpServletRequestBuilder request, String body) {
        return as(token, request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    long createTask(String name, int effort, String skillsJson) throws Exception {
        String body = mvc.perform(json(adminToken, post("/api/tasks"),
                        "{\"name\":\"%s\",\"effortHours\":%d,\"skills\":%s,\"status\":\"NOT_DONE\"}"
                                .formatted(name, effort, skillsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    long createWorker(String name, int availability, String skillsJson) throws Exception {
        String body = mvc.perform(json(adminToken, post("/api/workers"),
                        "{\"name\":\"%s\",\"availabilityHoursPerDay\":%d,\"skills\":%s}"
                                .formatted(name, availability, skillsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
