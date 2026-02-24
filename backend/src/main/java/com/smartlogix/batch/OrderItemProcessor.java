package com.smartlogix.batch;

import com.smartlogix.domain.entity.Order;
import com.smartlogix.domain.entity.Tenant;
import com.smartlogix.domain.enums.OrderStatus;
import com.smartlogix.domain.repository.TenantRepository;
import com.smartlogix.dto.OrderCsvRecord;
import com.smartlogix.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderItemProcessor implements ItemProcessor<OrderCsvRecord, Order> {

    private final TenantRepository tenantRepository;

    /**
     * Transforms a raw CSV record into a persistable {@link Order} entity.
     * <p>
     * Processing logic:
     * <ol>
     *   <li>Returns {@code null} (causing Spring Batch to skip the item) when the
     *       {@code orderNumber} field is blank or missing.</li>
     *   <li>Maps {@code orderNumber}, {@code description}, and {@code destinationAddress}
     *       directly from the CSV record.</li>
     *   <li>Sets the initial order status to {@link OrderStatus#PENDING}.</li>
     *   <li>Parses the {@code weight} field as a {@link java.math.BigDecimal}; logs a warning
     *       and leaves the field unset if the value is not a valid number.</li>
     *   <li>Reads the current tenant from {@link TenantContext} and associates the order with
     *       the active tenant entity if one is available.</li>
     * </ol>
     * </p>
     *
     * @param record the deserialized CSV row to process
     * @return the mapped {@link Order} entity ready to be written, or {@code null} to skip the record
     */
    @Override
    public Order process(OrderCsvRecord record) {
        if (record.getOrderNumber() == null || record.getOrderNumber().isBlank()) {
            log.warn("Skipping CSV record with blank orderNumber");
            return null;
        }

        Order order = new Order();
        order.setOrderNumber(record.getOrderNumber());
        order.setDescription(record.getDescription());
        order.setDestinationAddress(record.getDestinationAddress());
        order.setStatus(OrderStatus.PENDING);

        if (record.getWeight() != null && !record.getWeight().isBlank()) {
            try {
                order.setWeight(new BigDecimal(record.getWeight()));
            } catch (NumberFormatException e) {
                log.warn("Invalid weight value '{}' for order {}", record.getWeight(), record.getOrderNumber());
            }
        }

        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            tenantRepository.findByIdAndActiveTrue(tenantId).ifPresent(order::setTenant);
        }

        return order;
    }
}
