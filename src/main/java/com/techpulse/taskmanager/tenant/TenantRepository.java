package com.techpulse.taskmanager.tenant;

import org.springframework.data.repository.Repository;

/**
 * Extends the bare {@link Repository} marker (not JpaRepository) so that no unscoped
 * finder like findAll() exists. Tenants are only ever created and name-checked at registration.
 */
public interface TenantRepository extends Repository<Tenant, Long> {

    Tenant save(Tenant tenant);

    boolean existsByNameIgnoreCase(String name);
}
