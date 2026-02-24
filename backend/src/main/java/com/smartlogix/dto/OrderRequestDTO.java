package com.smartlogix.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for creating a new order via {@code POST /api/orders}.
 * <p>
 * {@code orderNumber} is the only mandatory field; all other fields are optional and can
 * be omitted or filled in later by updating the order.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    /** Unique human-readable identifier for the order. Must not be blank. */
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    /** Optional free-text description of the shipment contents. */
    private String description;

    /** Optional full delivery address (street, city, country). */
    private String destinationAddress;

    /** Optional gross weight of the shipment in kilograms. */
    private BigDecimal weight;

    /** Optional WGS-84 latitude of the shipment's current or destination location. */
    private Double latitude;

    /** Optional WGS-84 longitude of the shipment's current or destination location. */
    private Double longitude;

    /** Optional free-text tracking or handling notes. */
    private String trackingNotes;
}
