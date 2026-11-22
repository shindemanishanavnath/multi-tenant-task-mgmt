package com.techpulse.taskmanager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes for HS256");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("jwt.expiration-minutes must be positive");
        }
    }
}
