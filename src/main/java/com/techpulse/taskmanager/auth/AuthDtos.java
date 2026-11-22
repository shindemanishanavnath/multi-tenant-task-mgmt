package com.techpulse.taskmanager.auth;

import com.techpulse.taskmanager.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken,
                                String tokenType,
                                long expiresIn,
                                Long userId,
                                Long tenantId,
                                Role role) {
    }
}
