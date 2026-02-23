package com.smartlogix.service;

import com.smartlogix.domain.entity.Order;
import com.smartlogix.domain.entity.Tenant;
import com.smartlogix.domain.entity.User;
import com.smartlogix.domain.enums.OrderStatus;
import com.smartlogix.domain.enums.UserRole;
import com.smartlogix.domain.repository.OrderRepository;
import com.smartlogix.domain.repository.TenantRepository;
import com.smartlogix.domain.repository.UserRepository;
import com.smartlogix.dto.OrderRequestDTO;
import com.smartlogix.dto.OrderResponseDTO;
import com.smartlogix.exception.ResourceNotFoundException;
import com.smartlogix.mapper.OrderMapper;
import com.smartlogix.messaging.OrderEventProducer;
import com.smartlogix.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderService orderService;

    private UUID tenantId;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        tenant = Tenant.builder()
                .id(tenantId)
                .name("Test Tenant")
                .slug("test-tenant")
                .active(true)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .role(UserRole.ROLE_USER)
                .tenant(tenant)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createOrder_shouldReturnOrderResponseDTO() {
        OrderRequestDTO requestDTO = OrderRequestDTO.builder()
                .orderNumber("ORD-TEST-001")
                .description("Test description")
                .destinationAddress("123 Test St")
                .weight(new BigDecimal("10.0"))
                .build();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-TEST-001")
                .status(OrderStatus.PENDING)
                .tenant(tenant)
                .build();

        OrderResponseDTO responseDTO = OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber("ORD-TEST-001")
                .status(OrderStatus.PENDING)
                .tenantId(tenantId)
                .build();

        when(tenantRepository.findByIdAndActiveTrue(tenantId)).thenReturn(Optional.of(tenant));
        when(orderMapper.toEntity(requestDTO)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(order)).thenReturn(responseDTO);

        OrderResponseDTO result = orderService.createOrder(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).publishOrderEvent(order, "OrderCreated");
    }

    @Test
    void transitionStatus_validTransition_shouldSucceed() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-002")
                .status(OrderStatus.PENDING)
                .tenant(tenant)
                .build();

        Order updatedOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-002")
                .status(OrderStatus.APPROVED)
                .tenant(tenant)
                .build();

        OrderResponseDTO responseDTO = OrderResponseDTO.builder()
                .id(orderId)
                .orderNumber("ORD-002")
                .status(OrderStatus.APPROVED)
                .tenantId(tenantId)
                .build();

        when(orderRepository.findByTenantIdAndId(tenantId, orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);

        OrderResponseDTO result = orderService.transitionStatus(orderId, OrderStatus.APPROVED);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.APPROVED);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).publishOrderEvent(any(), eq("OrderStatusChanged"));
    }

    @Test
    void transitionStatus_invalidTransition_shouldThrowIllegalStateException() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-003")
                .status(OrderStatus.DELIVERED)
                .tenant(tenant)
                .build();

        when(orderRepository.findByTenantIdAndId(tenantId, orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.transitionStatus(orderId, OrderStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition from DELIVERED to APPROVED");
    }

    @Test
    void transitionStatus_cancelledOrder_shouldThrowIllegalStateException() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-004")
                .status(OrderStatus.CANCELLED)
                .tenant(tenant)
                .build();

        when(orderRepository.findByTenantIdAndId(tenantId, orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.transitionStatus(orderId, OrderStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition from CANCELLED to PENDING");
    }

    @Test
    void getOrderById_notFound_shouldThrowResourceNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByTenantIdAndId(tenantId, orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
