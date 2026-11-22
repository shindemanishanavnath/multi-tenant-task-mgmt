package com.techpulse.taskmanager.task;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant-scoped task repository. Every method requires a tenantId; there is deliberately
 * no findById()/findAll() (bare {@link Repository} instead of JpaRepository).
 */
public interface TaskRepository extends Repository<Task, Long> {

    Task save(Task task);

    Optional<Task> findByIdAndTenantId(Long id, Long tenantId);

    /** Ownership check for USER status updates: the task must be in the tenant AND assigned to them. */
    Optional<Task> findByIdAndTenantIdAndAssignedToId(Long id, Long tenantId, Long assignedToId);

    List<Task> findAllByTenantIdOrderByIdAsc(Long tenantId);

    List<Task> findAllByTenantIdAndAssignedToIdOrderByIdAsc(Long tenantId, Long assignedToId);
}
