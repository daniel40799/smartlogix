package com.smartlogix.domain.enums;

/**
 * Role assigned to a {@link com.smartlogix.domain.entity.User} in the SmartLogix platform.
 * <p>
 * Roles are stored as strings in the database and embedded as the {@code role} claim in the
 * JWT token. Spring Security reads the granted authority directly from the enum name, so
 * the {@code ROLE_} prefix follows the Spring Security naming convention for
 * {@link org.springframework.security.core.authority.SimpleGrantedAuthority}.
 * </p>
 */
public enum UserRole {
    /** Super-user within a tenant; has elevated privileges for administrative operations. */
    ROLE_ADMIN,

    /** Standard user within a tenant; can create and view orders but has no admin rights. */
    ROLE_USER
}
