package com.smartlogix.dto;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private UUID id;
    private String orderNumber;
    private String description;
    private OrderStatus status;
    private String destinationAddress;
    private BigDecimal weight;
    private Double latitude;
    private Double longitude;
    private String trackingNotes;
    private UUID tenantId;
    private Instant createdAt;
    private Instant updatedAt;
}
