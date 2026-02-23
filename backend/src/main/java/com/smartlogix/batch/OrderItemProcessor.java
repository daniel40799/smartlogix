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
