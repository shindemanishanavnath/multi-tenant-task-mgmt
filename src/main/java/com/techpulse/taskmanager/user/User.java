package com.techpulse.taskmanager.user;

import com.techpulse.taskmanager.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_tenant_id", columnList = "tenant_id"))
public class User extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Globally unique so that login can resolve the user (and therefore the tenant) from email alone. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash, never the raw password. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected User() {
    }

    public User(String email, String passwordHash, Role role, Long tenantId) {
        this.email = email;
        this.password = passwordHash;
        this.role = role;
        setTenantId(tenantId);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
