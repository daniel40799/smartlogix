package com.smartlogix.security;

import java.util.UUID;

/**
 * Thread-local store for the current request's tenant identifier.
 * <p>
 * SmartLogix uses a shared-database, shared-schema multi-tenancy strategy where every entity
 * carries a {@code tenant_id} foreign key. {@code TenantContext} acts as the per-request
 * carrier for this identifier, allowing service and repository layers to scope queries without
 * passing the tenant ID through every method signature.
 * </p>
 * <p>
 * The value is set by {@link com.smartlogix.security.JwtAuthFilter} at the start of each
 * authenticated HTTP request and <strong>must</strong> be cleared in a {@code finally} block
 * after the request is complete to prevent leakage across pooled threads.
 * </p>
 */
public class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Returns the tenant UUID stored for the current thread.
     *
     * @return the current tenant's {@link UUID}, or {@code null} if none has been set
     */
    public static UUID get() {
        return TENANT_ID.get();
    }

    /**
     * Stores the given tenant UUID in the current thread's context.
     * <p>
     * This is called by {@link com.smartlogix.security.JwtAuthFilter} after a valid JWT is
     * parsed, making the tenant ID available to the service and repository layers for the
     * lifetime of the current request.
     * </p>
     *
     * @param tenantId the {@link UUID} of the tenant associated with the current request
     */
    public static void set(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Removes the tenant UUID from the current thread's context.
     * <p>
     * <strong>Must</strong> be called in a {@code finally} block after each request to avoid
     * thread-local leaks when threads are reused from a pool (e.g., Tomcat thread pool).
     * </p>
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
