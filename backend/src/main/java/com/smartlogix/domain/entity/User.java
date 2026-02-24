package com.smartlogix.domain.entity;

import com.smartlogix.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing an authenticated user of the SmartLogix platform.
 * <p>
 * Implements {@link UserDetails} so it can be used directly by Spring Security's
 * authentication pipeline. Each user belongs to exactly one {@link Tenant} and holds
 * a single {@link UserRole} that determines their permissions.
 * </p>
 * <p>
 * Account expiry, locking, and credential expiry are not currently modelled —
 * the corresponding {@link UserDetails} methods return {@code true} unconditionally.
 * </p>
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user's email address, which acts as the login identifier and the JWT subject claim.
     * Must be globally unique across all tenants.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * BCrypt hash of the user's password.
     * The plain-text password is never stored; only the hash is persisted.
     */
    @Column(nullable = false)
    private String passwordHash;

    /** The role assigned to this user, controlling their access level within the platform. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * The tenant this user belongs to.
     * Loaded lazily; a user can only access data belonging to their own tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** UTC timestamp set automatically when the user record is first persisted. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** UTC timestamp updated automatically on every modification. */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Returns the granted authorities derived from this user's {@link UserRole}.
     * Spring Security uses these authorities for access-control decisions.
     *
     * @return a singleton list containing a {@link SimpleGrantedAuthority} for the user's role
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Returns the BCrypt-hashed password used by Spring Security for credential verification.
     *
     * @return the stored password hash
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Returns the username used by Spring Security, which in SmartLogix is the user's email.
     *
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the account has expired. Always returns {@code true} because account
     * expiry is not implemented in the current version.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the account is locked. Always returns {@code true} because account
     * locking is not implemented in the current version.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the credentials have expired. Always returns {@code true} because
     * credential expiry is not implemented in the current version.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user account is enabled. Always returns {@code true}; use
     * {@link Tenant#isActive()} to disable access at the tenant level.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
