package com.smartlogix.domain.repository;

import com.smartlogix.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Tenant} entities.
 * <p>
 * Provides lookups used during registration (by slug) and request processing
 * (by ID with active check).
 * </p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Looks up a tenant by its URL-friendly slug, used during user registration to
     * resolve or create the appropriate tenant context.
     *
     * @param slug the unique slug string (e.g. {@code acme-logistics})
     * @return an {@link Optional} containing the matching {@link Tenant}, or empty if none exists
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Looks up an active tenant by its UUID.
     * Returns an empty {@link Optional} if the tenant does not exist or has been deactivated.
     * Used by the service layer to enforce that only active tenants can perform operations.
     *
     * @param id the UUID of the tenant to look up
     * @return an {@link Optional} containing the active {@link Tenant}, or empty if not found
     */
    Optional<Tenant> findByIdAndActiveTrue(UUID id);
}
