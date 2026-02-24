package com.smartlogix.domain.enums;

/**
 * Lifecycle states of a {@link com.smartlogix.domain.entity.Order} within the SmartLogix
 * state machine.
 * <p>
 * Legal transitions (enforced by
 * {@link com.smartlogix.service.OrderService#validateTransition}):
 * <pre>
 *   PENDING → APPROVED → IN_TRANSIT → SHIPPED → DELIVERED
 *      └──────────┴───────────┴──────────┘
 *                       CANCELLED (terminal)
 * </pre>
 * {@code DELIVERED} and {@code CANCELLED} are terminal states — no further transitions
 * are permitted once an order reaches either of these statuses.
 * </p>
 */
public enum OrderStatus {
    /** The order has been submitted and is awaiting review. */
    PENDING,

    /** The order has been reviewed and approved for fulfilment. */
    APPROVED,

    /** The shipment is currently in transit between facilities. */
    IN_TRANSIT,

    /** The shipment has been handed over to the final carrier. */
    SHIPPED,

    /** The shipment has been successfully delivered to the destination. Terminal state. */
    DELIVERED,

    /** The order has been cancelled. Terminal state; no further transitions are allowed. */
    CANCELLED
}
