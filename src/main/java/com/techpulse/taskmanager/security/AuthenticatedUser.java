package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * The principal stored in the SecurityContext. Every field comes from verified JWT claims.
 */
public record AuthenticatedUser(Long userId, Long tenantId, String email, Role role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
