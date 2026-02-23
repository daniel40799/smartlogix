package com.smartlogix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    private String description;

    private String destinationAddress;

    private BigDecimal weight;

    private Double latitude;

    private Double longitude;

    private String trackingNotes;
}
