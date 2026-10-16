package com.jutinico.scheduler.auth;

import java.time.Instant;

import com.jutinico.scheduler.common.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50)
            @Pattern(regexp = ValidationPatterns.NO_CONTROL_CHARS, message = ValidationPatterns.NO_CONTROL_CHARS_MESSAGE) String username,
            @NotBlank @Size(max = 100) String password) {
    }

    public record LoginResponse(String token, String username, Role role, Instant expiresAt) {
    }
}
