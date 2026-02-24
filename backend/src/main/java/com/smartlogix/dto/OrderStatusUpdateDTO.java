package com.smartlogix.dto;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO used to represent an order status update event, typically originating from
 * a WebSocket notification or an internal status-change operation.
 * <p>
 * This DTO is separate from the REST PATCH body (which uses a plain {@code Map}) to
 * allow strongly-typed handling of status update events in internal service calls
 * or future messaging integrations.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDTO {

    /** UUID of the order whose status is being updated. */
    private UUID orderId;

    /** The target status to transition the order to. */
    private OrderStatus newStatus;

    /** UUID of the tenant that owns the order, used to scope the update correctly. */
    private UUID tenantId;
}
