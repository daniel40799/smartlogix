package com.smartlogix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket / STOMP message broker configuration for SmartLogix.
 * <p>
 * Enables a STOMP-over-WebSocket endpoint at {@code /ws} with SockJS fallback support.
 * The simple in-memory message broker serves destinations prefixed with {@code /topic},
 * and application-bound messages use the prefix {@code /app}. The React frontend
 * subscribes to {@code /topic/orders/{tenantId}} to receive live order status updates.
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker.
     * <p>
     * <ul>
     *   <li>Enables a simple in-memory broker for destinations starting with
     *       {@code /topic}.</li>
     *   <li>Sets {@code /app} as the application destination prefix — messages sent
     *       from clients to {@code /app/...} are routed to
     *       {@code @MessageMapping}-annotated controller methods.</li>
     * </ul>
     * </p>
     *
     * @param registry the {@link MessageBrokerRegistry} to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the STOMP WebSocket endpoint.
     * <p>
     * The endpoint is exposed at {@code /ws} with SockJS fallback enabled so that
     * browsers without native WebSocket support can still connect via long-polling or
     * other transports. All origin patterns are allowed to support local development
     * (adjust to specific origins in production).
     * </p>
     *
     * @param registry the {@link StompEndpointRegistry} to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
