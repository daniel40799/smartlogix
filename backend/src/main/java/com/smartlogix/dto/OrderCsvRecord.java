package com.smartlogix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCsvRecord {

    private String orderNumber;
    private String description;
    private String destinationAddress;
    private String weight;
}
