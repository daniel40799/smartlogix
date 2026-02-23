package com.smartlogix.dto;

import com.smartlogix.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDTO {

    private UUID orderId;
    private OrderStatus newStatus;
    private UUID tenantId;
}
