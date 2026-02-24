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
public class OrderEventConsumerConfig {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Declares the Spring Cloud Stream consumer function that processes inbound
     * {@link OrderEvent} messages from the Kafka {@code order-events} topic.
     * <p>
     * For each received event, the consumer:
     * <ol>
     *   <li>Logs the event type, order ID, and tenant ID for observability.</li>
     *   <li>Derives the tenant-specific WebSocket destination:
     *       {@code /topic/orders/{tenantId}}.</li>
     *   <li>Sends the event payload to that destination via
     *       {@link SimpMessagingTemplate}, delivering a real-time push notification to all
     *       React clients subscribed to that tenant's channel.</li>
     * </ol>
     * The bean name {@code orderEventConsumer} matches the Spring Cloud Stream function
     * binding convention ({@code spring.cloud.stream.function.definition=orderEventConsumer}).
     * </p>
     *
     * @return a {@link java.util.function.Consumer} that handles incoming {@link OrderEvent} messages
     */
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
