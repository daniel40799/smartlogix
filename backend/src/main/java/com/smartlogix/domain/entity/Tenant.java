package com.smartlogix.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a tenant (company) in the SmartLogix multi-tenant platform.
 * <p>
 * SmartLogix uses a shared-database, shared-schema multi-tenancy strategy: every
 * {@link Order} and {@link User} row is scoped to a tenant via a {@code tenant_id}
 * foreign key. A tenant must be {@link #active} for its data to be accessible;
 * deactivated tenants are effectively soft-deleted without removing their data.
 * </p>
 */
@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable display name of the company. Must be unique across all tenants. */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * URL-friendly unique identifier for the tenant (e.g. {@code acme-logistics}).
     * Used during registration and in API paths for tenant resolution.
     */
    @Column(unique = true, nullable = false)
    private String slug;

    /**
     * Whether this tenant is currently active.
     * Inactive tenants are rejected by service-layer tenant lookups, effectively
     * preventing login and data access without deleting the tenant record.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** UTC timestamp set automatically when the tenant record is first persisted. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** UTC timestamp updated automatically on every modification. */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * All orders belonging to this tenant.
     * Cascade {@code ALL} and {@code orphanRemoval = true} ensure that deleting a tenant
     * removes its orders from the database.
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    /**
     * All user accounts belonging to this tenant.
     * Cascade {@code ALL} and {@code orphanRemoval = true} ensure that deleting a tenant
     * removes all of its user records.
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();
}
