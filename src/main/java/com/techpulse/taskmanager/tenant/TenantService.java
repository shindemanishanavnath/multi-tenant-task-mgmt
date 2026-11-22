package com.techpulse.taskmanager.tenant;

import com.techpulse.taskmanager.common.ApiException;
import com.techpulse.taskmanager.tenant.TenantDtos.RegisterTenantRequest;
import com.techpulse.taskmanager.tenant.TenantDtos.RegisterTenantResponse;
import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import com.techpulse.taskmanager.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates the tenant and its first ADMIN in one transaction. */
    @Transactional
    public RegisterTenantResponse register(RegisterTenantRequest request) {
        String tenantName = request.tenantName().trim();
        String adminEmail = request.adminEmail().trim().toLowerCase(Locale.ROOT);

        if (tenantRepository.existsByNameIgnoreCase(tenantName)) {
            throw ApiException.conflict("A tenant with this name already exists");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            throw ApiException.conflict("A user with this email already exists");
        }

        Tenant tenant = tenantRepository.save(new Tenant(tenantName));
        User admin = userRepository.save(new User(adminEmail,
                passwordEncoder.encode(request.adminPassword()), Role.ADMIN, tenant.getId()));

        return new RegisterTenantResponse(tenant.getId(), tenant.getName(), tenant.getCreatedAt(),
                admin.getId(), admin.getEmail());
    }
}
