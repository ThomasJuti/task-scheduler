package com.jutinico.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

class ProblemJsonHandlersTest {

    private final ProblemJsonHandlers handlers = new ProblemJsonHandlers(JsonMapper.builder().build());

    @Test
    void writesAProblemJsonBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.unauthorized().commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        String body = response.getContentAsString();
        assertThat((String) JsonPath.read(body, "$.title")).isEqualTo("Unauthorized");
        assertThat((Integer) JsonPath.read(body, "$.status")).isEqualTo(401);
        assertThat((String) JsonPath.read(body, "$.detail")).startsWith("Authentication is required");
        // Same shape as the MVC error responses: empty fields are omitted (a missing "type" means about:blank)
        assertThat(body).doesNotContain("\"type\"").doesNotContain("instance").doesNotContain("properties");
    }

    @Test
    void escapesTheDetail() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String detail = "a \"quoted\" \\ value\nwith a new line";

        handlers.write(response, HttpStatus.FORBIDDEN, detail);

        assertThat((String) JsonPath.read(response.getContentAsString(), "$.detail")).isEqualTo(detail);
    }
}
