package com.jutinico.scheduler.auth;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import com.jutinico.scheduler.auth.AuthDtos.LoginResponse;
import com.jutinico.scheduler.common.TooManyRequestsException;
import com.jutinico.scheduler.common.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttempts;
    /** Compared against when the user does not exist, so both cases take the same time. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
                       LoginAttemptService loginAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password, String clientIp) {
        String key = username.trim().toLowerCase(Locale.ROOT) + "|" + clientIp;
        Duration locked = loginAttempts.lockRemaining(key);
        if (!locked.isZero()) {
            throw new TooManyRequestsException("Too many failed attempts. Try again in "
                    + Math.max(1, locked.toMinutes()) + " minute(s)");
        }

        Optional<AppUser> user = users.findByUsername(username.trim());
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches) {
            loginAttempts.recordFailure(key);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        loginAttempts.reset(key);
        return tokenService.issue(user.get());
    }
}
