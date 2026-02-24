package com.smartlogix.domain.repository;

import com.smartlogix.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 * <p>
 * Provides user lookups used during authentication and registration.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Looks up a user by email address scoped to a specific tenant.
     * Used during registration to detect duplicate users within the same tenant, and
     * during order creation to resolve the {@code createdBy} relationship.
     *
     * @param email    the user's email address
     * @param tenantId the UUID of the tenant to scope the lookup to
     * @return an {@link Optional} containing the matching {@link User}, or empty if none exists
     */
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Looks up a user by email address globally (across all tenants).
     * Used by {@link com.smartlogix.security.CustomUserDetailsService} during Spring Security
     * authentication, where only the email (JWT subject) is known.
     *
     * @param email the user's email address
     * @return an {@link Optional} containing the matching {@link User}, or empty if none exists
     */
    Optional<User> findByEmail(String email);
}
