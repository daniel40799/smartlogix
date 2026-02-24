package com.smartlogix.dto;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only response payload returned for order-related API calls.
 * <p>
 * Produced by {@link com.smartlogix.mapper.OrderMapper#toResponseDTO(com.smartlogix.domain.entity.Order)}
 * and returned by all order endpoints. Contains all publicly visible order fields including
 * the tenant identifier (flattened from the nested entity), timestamps, and current status.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    /** UUID of the order record. */
    private UUID id;

    /** Human-readable order identifier assigned at creation time. */
    private String orderNumber;

    /** Free-text description of the shipment contents. */
    private String description;

    /** Current lifecycle status of the order. */
    private OrderStatus status;

    /** Full destination address string. */
    private String destinationAddress;

    /** Gross weight of the shipment in kilograms. */
    private BigDecimal weight;

    /** WGS-84 latitude for map display (may be {@code null} if not provided). */
    private Double latitude;

    /** WGS-84 longitude for map display (may be {@code null} if not provided). */
    private Double longitude;

    /** Free-text tracking or handling notes. */
    private String trackingNotes;

    /** UUID of the tenant that owns this order (flattened from the {@code tenant.id} association). */
    private UUID tenantId;

    /** UTC timestamp of when the order was first persisted. */
    private Instant createdAt;

    /** UTC timestamp of the most recent modification. */
    private Instant updatedAt;
}
