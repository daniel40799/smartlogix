package com.smartlogix.controller;

import com.smartlogix.domain.enums.OrderStatus;
import com.smartlogix.domain.repository.OrderRepository;
import com.smartlogix.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Metrics", description = "Order metrics endpoints")
public class MetricsController {

    private final OrderRepository orderRepository;

    /**
     * Returns a summary of order counts grouped by status for the currently authenticated tenant.
     * <p>
     * Iterates over all {@link OrderStatus} values and queries the repository for the count of
     * orders in each state, scoped to the tenant derived from {@link TenantContext}. The result
     * is suitable for rendering dashboard widgets (e.g., a status breakdown chart) in the React
     * frontend.
     * </p>
     *
     * @return {@link ResponseEntity} containing a map with keys {@code tenantId} (the resolved
     *         tenant UUID as a string) and {@code ordersByStatus} (a map of status → count)
     */
    @GetMapping("/summary")
    @Operation(summary = "Get order count summary by status for current tenant")
    public ResponseEntity<Map<String, Object>> getSummary() {
        UUID tenantId = TenantContext.get();
        String tenantTag = tenantId != null ? tenantId.toString() : "unknown";
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);

        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByTenantIdAndStatus(tenantId, status);
            statusCounts.put(status, count);
        }

        return ResponseEntity.ok(Map.of(
                "tenantId", tenantTag,
                "ordersByStatus", statusCounts
        ));
    }
}
