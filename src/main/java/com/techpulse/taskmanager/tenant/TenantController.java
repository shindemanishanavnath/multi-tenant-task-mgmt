package com.techpulse.taskmanager.tenant;

import com.techpulse.taskmanager.tenant.TenantDtos.RegisterTenantRequest;
import com.techpulse.taskmanager.tenant.TenantDtos.RegisterTenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** Public: creates a tenant and its first ADMIN user. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterTenantResponse register(@Valid @RequestBody RegisterTenantRequest request) {
        return tenantService.register(request);
    }
}
