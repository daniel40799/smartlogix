package com.smartlogix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single row read from a CSV order import file.
 * <p>
 * Spring Batch's {@link org.springframework.batch.item.file.FlatFileItemReader} maps
 * CSV columns to these fields via
 * {@link org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper}.
 * The expected CSV column order is: {@code orderNumber, description, destinationAddress, weight}.
 * </p>
 * <p>
 * All fields are kept as {@link String} at this stage; type conversion and validation
 * are performed by {@link com.smartlogix.batch.OrderItemProcessor}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCsvRecord {

    /** Unique order identifier as read from the CSV (e.g. {@code ORD-2024-001}). */
    private String orderNumber;

    /** Free-text description of the shipment contents. */
    private String description;

    /** Destination address string for the shipment. */
    private String destinationAddress;

    /** Raw weight value string from the CSV; parsed to {@link java.math.BigDecimal} by the processor. */
    private String weight;
}
