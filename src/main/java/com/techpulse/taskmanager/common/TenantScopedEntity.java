package com.techpulse.taskmanager.common;

import com.techpulse.taskmanager.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Base class for every entity that belongs to a tenant.
 * <p>
 * Last line of defence before the database: an entity can never be written without a tenant,
 * and never under a tenant different from the one bound to the current request.
 */
@MappedSuperclass
public abstract class TenantScopedEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    @PreUpdate
    public void verifyTenantOwnership() {
        if (tenantId == null) {
            throw new IllegalStateException("tenant_id is required on " + getClass().getSimpleName());
        }
        Long currentTenant = TenantContext.getTenantId();
        if (currentTenant != null && !currentTenant.equals(tenantId)) {
            throw new IllegalStateException("Cross-tenant write blocked on " + getClass().getSimpleName());
        }
    }
}
