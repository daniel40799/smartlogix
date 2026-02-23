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
