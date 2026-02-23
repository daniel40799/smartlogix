package com.smartlogix.messaging;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String eventType;
    private UUID orderId;
    private UUID tenantId;
    private OrderStatus status;
    private Instant timestamp;
}
