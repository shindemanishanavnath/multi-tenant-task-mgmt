package com.techpulse.taskmanager.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TenantDtos {

    private TenantDtos() {
    }

    public record RegisterTenantRequest(
            @NotBlank @Size(max = 100) String tenantName,
            @NotBlank @Email @Size(max = 255) String adminEmail,
            @NotBlank @Size(min = 8, max = 100) String adminPassword) {
    }

    public record RegisterTenantResponse(Long tenantId,
                                         String tenantName,
                                         Instant createdAt,
                                         Long adminUserId,
                                         String adminEmail) {
    }
}
