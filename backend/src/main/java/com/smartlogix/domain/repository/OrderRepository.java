package com.smartlogix.domain.repository;

import com.smartlogix.domain.entity.Order;
import com.smartlogix.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, RevisionRepository<Order, UUID, Long> {

    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);

    Optional<Order> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByTenantIdAndStatus(UUID tenantId, OrderStatus status);
}
