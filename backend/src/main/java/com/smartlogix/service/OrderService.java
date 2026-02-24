package com.smartlogix.service;

import com.smartlogix.domain.entity.Order;
import com.smartlogix.domain.entity.Tenant;
import com.smartlogix.domain.entity.User;
import com.smartlogix.domain.enums.OrderStatus;
import com.smartlogix.domain.repository.OrderRepository;
import com.smartlogix.domain.repository.TenantRepository;
import com.smartlogix.domain.repository.UserRepository;
import com.smartlogix.dto.OrderRequestDTO;
import com.smartlogix.dto.OrderResponseDTO;
import com.smartlogix.exception.ResourceNotFoundException;
import com.smartlogix.mapper.OrderMapper;
import com.smartlogix.messaging.OrderEventProducer;
import com.smartlogix.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a new order for the currently authenticated tenant.
     * <p>
     * Resolves the active {@link com.smartlogix.domain.entity.Tenant} from the current
     * {@link TenantContext}, maps the incoming DTO to an {@link Order} entity, sets the
     * initial status to {@link com.smartlogix.domain.enums.OrderStatus#PENDING}, optionally
     * links the authenticated user as the creator, persists the entity, and publishes an
     * {@code OrderCreated} event to Kafka via {@link OrderEventProducer}.
     * </p>
     *
     * @param requestDTO the validated order creation payload containing order details
     * @return an {@link OrderResponseDTO} representing the newly persisted order
     * @throws ResourceNotFoundException if the tenant resolved from the context is not found or inactive
     */
    public OrderResponseDTO createOrder(OrderRequestDTO requestDTO) {
        UUID tenantId = TenantContext.get();
        Tenant tenant = tenantRepository.findByIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Order order = orderMapper.toEntity(requestDTO);
        order.setTenant(tenant);
        order.setStatus(OrderStatus.PENDING);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            userRepository.findByEmailAndTenantId(auth.getName(), tenantId)
                    .ifPresent(order::setCreatedBy);
        }

        Order saved = orderRepository.save(order);
        log.info("Created order: id={}, orderNumber={}, tenantId={}", saved.getId(), saved.getOrderNumber(), tenantId);

        orderEventProducer.publishOrderEvent(saved, "OrderCreated");

        return orderMapper.toResponseDTO(saved);
    }

    /**
     * Returns a paginated list of orders belonging to the currently authenticated tenant.
     * <p>
     * The tenant identifier is read from {@link TenantContext} so that Company A's data is
     * never mixed with Company B's data (row-level multi-tenancy).
     * </p>
     *
     * @param pageable Spring Data pagination and sorting parameters
     * @return a {@link Page} of {@link OrderResponseDTO} objects scoped to the current tenant
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrders(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return orderRepository.findByTenantId(tenantId, pageable)
                .map(orderMapper::toResponseDTO);
    }

    /**
     * Retrieves a single order by its UUID, scoped to the current tenant.
     * <p>
     * Ensures that cross-tenant access is prevented by filtering on both {@code tenant_id}
     * and the provided {@code id}.
     * </p>
     *
     * @param id the UUID of the order to retrieve
     * @return an {@link OrderResponseDTO} for the matching order
     * @throws ResourceNotFoundException if no order with the given ID exists for the current tenant
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        UUID tenantId = TenantContext.get();
        Order order = orderRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return orderMapper.toResponseDTO(order);
    }

    /**
     * Transitions an order to a new status following the enforced state-machine rules.
     * <p>
     * Valid transitions are:
     * <pre>
     *   PENDING    → APPROVED | CANCELLED
     *   APPROVED   → IN_TRANSIT | CANCELLED
     *   IN_TRANSIT → SHIPPED | CANCELLED
     *   SHIPPED    → DELIVERED
     *   DELIVERED  → (terminal — no further transitions)
     *   CANCELLED  → (terminal — no further transitions)
     * </pre>
     * After a successful transition the updated order is saved, an {@code OrderStatusChanged}
     * Kafka event is published, and a WebSocket notification is broadcast to
     * {@code /topic/orders/{tenantId}} so connected React clients receive the update in real time.
     * </p>
     *
     * @param orderId   the UUID of the order to transition
     * @param newStatus the desired target status
     * @return an {@link OrderResponseDTO} reflecting the updated status
     * @throws ResourceNotFoundException if the order does not exist for the current tenant
     * @throws IllegalStateException     if the requested status transition is not allowed
     */
    public OrderResponseDTO transitionStatus(UUID orderId, OrderStatus newStatus) {
        UUID tenantId = TenantContext.get();
        Order order = orderRepository.findByTenantIdAndId(tenantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        validateTransition(order.getStatus(), newStatus);
        OrderStatus previousStatus = order.getStatus();

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("Order status transitioned: id={}, from={}, to={}", orderId, previousStatus, newStatus);

        orderEventProducer.publishOrderEvent(saved, "OrderStatusChanged");

        messagingTemplate.convertAndSend(
                "/topic/orders/" + tenantId,
                orderMapper.toResponseDTO(saved)
        );

        return orderMapper.toResponseDTO(saved);
    }

    /**
     * Validates that the requested status transition is permitted by the order state machine.
     * <p>
     * Terminal states ({@code DELIVERED} and {@code CANCELLED}) do not allow any further
     * transitions. All other invalid combinations (e.g. {@code PENDING → SHIPPED}) also
     * result in an exception.
     * </p>
     *
     * @param current the order's present {@link OrderStatus}
     * @param next    the desired target {@link OrderStatus}
     * @throws IllegalStateException if the transition from {@code current} to {@code next} is not allowed
     */
    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.APPROVED || next == OrderStatus.CANCELLED;
            case APPROVED -> next == OrderStatus.IN_TRANSIT || next == OrderStatus.CANCELLED;
            case IN_TRANSIT -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", current, next)
            );
        }
    }
}
