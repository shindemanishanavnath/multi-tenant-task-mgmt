package com.techpulse.taskmanager.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    /** Note: no tenant field. The tenant always comes from the caller's JWT. */
    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role) {
    }

    public record UserResponse(Long id, String email, Role role, Long tenantId) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
        }
    }
}
