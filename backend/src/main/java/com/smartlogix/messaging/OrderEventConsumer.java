package com.smartlogix.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @Bean
    public Consumer<OrderEvent> orderEventConsumer() {
        return event -> {
            log.info("Received order event: type={}, orderId={}, tenantId={}",
                    event.getEventType(), event.getOrderId(), event.getTenantId());

            String destination = "/topic/orders/" + event.getTenantId();
            messagingTemplate.convertAndSend(destination, event);

            log.debug("WebSocket notification sent to {}", destination);
        };
    }
}
