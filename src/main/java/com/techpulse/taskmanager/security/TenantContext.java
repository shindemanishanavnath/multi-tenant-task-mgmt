package com.techpulse.taskmanager.security;

/**
 * Holds the current request's tenant id for the lifetime of the request thread.
 * <p>
 * Populated only by {@link JwtAuthenticationFilter} from a verified JWT and always cleared
 * in a {@code finally} block, so a pooled thread never leaks one tenant into the next request.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /** Returns the tenant id, or {@code null} for unauthenticated requests. */
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /** Returns the tenant id or fails fast; used by every tenant-scoped service call. */
    public static Long requireTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant bound to the current request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
