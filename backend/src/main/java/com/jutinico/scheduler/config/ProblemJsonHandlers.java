package com.jutinico.scheduler.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 / 403 responses produced by the security filters, in the same problem+json shape as the API errors.
 * They run before Spring MVC, so the body is serialized here with the application's Jackson mapper.
 */
@Component
class ProblemJsonHandlers {

    private final JsonMapper mapper;

    ProblemJsonHandlers(JsonMapper mapper) {
        // Same mix-in Spring MVC uses for ProblemDetail: omits empty fields (instance, properties)
        this.mapper = mapper.rebuild().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build();
    }

    AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> write(response, HttpStatus.UNAUTHORIZED,
                "Authentication is required: missing, invalid or expired token");
    }

    AccessDeniedHandler forbidden() {
        return (request, response, ex) -> write(response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action");
    }

    void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
