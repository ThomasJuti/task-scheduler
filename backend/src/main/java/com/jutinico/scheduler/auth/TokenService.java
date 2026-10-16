package com.jutinico.scheduler.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.jutinico.scheduler.auth.AuthDtos.LoginResponse;
import com.jutinico.scheduler.config.AppSecurityProperties;
import com.jutinico.scheduler.config.SecurityConfig;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AppSecurityProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, AppSecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public LoginResponse issue(AppUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwtTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, user.getUsername(), user.getRole(), expiresAt);
    }
}
