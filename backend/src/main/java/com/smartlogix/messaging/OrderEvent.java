package com.smartlogix.messaging;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event payload published to Kafka and broadcast over WebSocket whenever
 * an order is created or its status changes.
 * <p>
 * The event is serialised to JSON by Jackson when sent to the {@code order-events-out-0}
 * Spring Cloud Stream binding, and deserialised on the consumer side by
 * {@link OrderEventConsumerConfig}. The same object is forwarded directly to the
 * WebSocket destination {@code /topic/orders/{tenantId}} so that React clients receive
 * real-time updates.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    /**
     * Discriminator string identifying the type of domain event.
     * Current values: {@code "OrderCreated"}, {@code "OrderStatusChanged"}.
     */
    private String eventType;

    /** UUID of the order that triggered the event. */
    private UUID orderId;

    /** UUID of the tenant that owns the order; used for WebSocket topic routing. */
    private UUID tenantId;

    /** The order's status at the time the event was emitted. */
    private OrderStatus status;

    /** UTC instant at which the event was created by {@link OrderEventProducer}. */
    private Instant timestamp;
}
