package com.smartlogix.domain.entity;

import com.smartlogix.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a logistics order in the SmartLogix platform.
 * <p>
 * Each order belongs to exactly one {@link Tenant} (multi-tenancy) and follows a strict
 * lifecycle enforced by the {@link com.smartlogix.service.OrderService} state machine:
 * <pre>
 *   PENDING → APPROVED → IN_TRANSIT → SHIPPED → DELIVERED
 *      └──────────┴───────────┴──────────┘
 *                       CANCELLED (terminal)
 * </pre>
 * The {@code @Audited} annotation triggers Hibernate Envers to snapshot every change
 * to an {@code orders_aud} audit table for compliance and traceability.
 * {@code @EntityListeners(AuditingEntityListener.class)} automatically populates
 * {@code createdAt} and {@code updatedAt} timestamps.
 * </p>
 */
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /** Auto-generated UUID primary key for the order. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable, unique identifier assigned by the user (e.g. {@code ORD-2024-001}). */
    @Column(unique = true, nullable = false)
    private String orderNumber;

    /** Free-text description of the shipment contents. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle status of the order.
     * Defaults to {@link OrderStatus#PENDING} for all newly created orders.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * The tenant (company) this order belongs to.
     * Loaded lazily; the association itself is not audited ({@code NOT_AUDITED}) because
     * tenant data changes independently from order data.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Tenant tenant;

    /**
     * The user who originally created this order.
     * Optional — may be {@code null} for system-generated (batch/FTP) orders.
     * The user association is not audited to avoid auditing unrelated user-profile changes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User createdBy;

    /** WGS-84 latitude of the current or destination location (may be {@code null}). */
    private Double latitude;

    /** WGS-84 longitude of the current or destination location (may be {@code null}). */
    private Double longitude;

    /** Full destination address string for display and routing purposes. */
    @Column(columnDefinition = "TEXT")
    private String destinationAddress;

    /** Gross weight of the shipment in kilograms, stored with up to 2 decimal places. */
    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    /** Free-text field for additional tracking or handling notes. */
    @Column(columnDefinition = "TEXT")
    private String trackingNotes;

    /** UTC timestamp set automatically by JPA auditing when the record is first persisted. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** UTC timestamp updated automatically by JPA auditing on every modification. */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
