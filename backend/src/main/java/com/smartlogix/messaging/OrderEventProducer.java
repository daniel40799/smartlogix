package com.smartlogix.messaging;

import com.smartlogix.domain.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final StreamBridge streamBridge;

    /**
     * Builds an {@link OrderEvent} from the supplied {@link Order} and publishes it to the
     * {@code order-events-out-0} Spring Cloud Stream binding (backed by Apache Kafka).
     * <p>
     * The event carries the following fields:
     * <ul>
     *   <li>{@code eventType} — discriminator string, e.g. {@code OrderCreated} or
     *       {@code OrderStatusChanged}</li>
     *   <li>{@code orderId} — UUID of the affected order</li>
     *   <li>{@code tenantId} — UUID of the owning tenant (used by consumers for tenant-scoped
     *       WebSocket routing)</li>
     *   <li>{@code status} — current {@link com.smartlogix.domain.enums.OrderStatus} of the order</li>
     *   <li>{@code timestamp} — UTC instant at which the event was created</li>
     * </ul>
     * Consumers of the {@code order-events-out-0} topic (see
     * {@link com.smartlogix.messaging.OrderEventConsumerConfig}) receive these events and
     * broadcast them via WebSocket to subscribed frontend clients.
     * </p>
     *
     * @param order     the order entity whose state should be captured in the event
     * @param eventType a descriptive string identifying the type of domain event
     */
    public void publishOrderEvent(Order order, String eventType) {
        OrderEvent event = OrderEvent.builder()
                .eventType(eventType)
                .orderId(order.getId())
                .tenantId(order.getTenant().getId())
                .status(order.getStatus())
                .timestamp(Instant.now())
                .build();

        log.info("Publishing order event: type={}, orderId={}, tenantId={}",
                eventType, event.getOrderId(), event.getTenantId());

        streamBridge.send("order-events-out-0", event);
    }
}
