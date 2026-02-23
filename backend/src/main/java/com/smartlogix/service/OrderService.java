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

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrders(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return orderRepository.findByTenantId(tenantId, pageable)
                .map(orderMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        UUID tenantId = TenantContext.get();
        Order order = orderRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return orderMapper.toResponseDTO(order);
    }

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
