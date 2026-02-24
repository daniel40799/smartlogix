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

/**
 * Spring Data JPA repository for {@link Order} entities.
 * <p>
 * Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link RevisionRepository} to expose the Hibernate Envers audit history for each order.
 * All derived query methods are automatically scoped to a single tenant via the
 * {@code tenantId} parameter to prevent cross-tenant data leakage.
 * </p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, RevisionRepository<Order, UUID, Long> {

    /**
     * Returns a paginated list of all orders belonging to the specified tenant.
     *
     * @param tenantId the UUID of the tenant whose orders to retrieve
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of {@link Order} objects for the given tenant
     */
    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Returns a paginated list of orders for a specific tenant filtered by status.
     *
     * @param tenantId the UUID of the owning tenant
     * @param status   the {@link OrderStatus} to filter by
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of matching {@link Order} objects
     */
    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);

    /**
     * Looks up a single order by its UUID, ensuring it belongs to the given tenant.
     * Returns an empty {@link Optional} if either the order does not exist or belongs to a
     * different tenant.
     *
     * @param tenantId the UUID of the expected owning tenant
     * @param id       the UUID of the order to retrieve
     * @return an {@link Optional} containing the matching order, or empty if not found
     */
    Optional<Order> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Counts the number of orders for a tenant that are currently in the given status.
     * Used by the metrics endpoint to build the order-count summary.
     *
     * @param tenantId the UUID of the owning tenant
     * @param status   the {@link OrderStatus} to count
     * @return the number of orders with the specified status for the tenant
     */
    long countByTenantIdAndStatus(UUID tenantId, OrderStatus status);
}
