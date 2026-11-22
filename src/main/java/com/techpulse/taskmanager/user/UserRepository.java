package com.techpulse.taskmanager.user;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant-scoped user repository.
 * <p>
 * Extends the bare {@link Repository} marker instead of JpaRepository, so unscoped methods such as
 * findAll() / findById() / deleteById() do not exist and cannot be called by mistake.
 * Every lookup takes a tenantId, except the two documented pre-authentication methods.
 */
public interface UserRepository extends Repository<User, Long> {

    User save(User user);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findAllByTenantIdOrderByIdAsc(Long tenantId);

    /**
     * Pre-authentication only (login): the tenant is not known until the user is found.
     * Emails are globally unique, so this can match at most one user.
     */
    Optional<User> findByEmail(String email);

    /** Global uniqueness check for email; returns a boolean only, never another tenant's data. */
    boolean existsByEmail(String email);
}
