import React, { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { fetchOrders } from '../store/slices/ordersSlice'
import type { OrderStatus } from '../types'

/**
 * Display order of status columns in the stats grid.
 * Mirrors the canonical order of the {@link OrderStatus} state machine.
 */
const STATUS_ORDER: OrderStatus[] = [
  'PENDING',
  'APPROVED',
  'IN_TRANSIT',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
]

/**
 * Dashboard page component — the default authenticated landing page.
 *
 * Renders:
 * - A greeting and a quick link to create a new order.
 * - A stats grid showing the count of orders in each status for the current tenant.
 * - A "Recent Orders" table showing the 8 most recently updated orders.
 * - A "Live Notifications" panel populated in real time by WebSocket events.
 *
 * Orders are fetched on mount via {@link fetchOrders} with a page size of 50 to
 * provide a reasonable overview without exhaustive pagination.
 */
const DashboardPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const { items, loading } = useAppSelector((state) => state.orders)
  const notifications = useAppSelector((state) => state.notifications.items)
  const email = useAppSelector((state) => state.auth.email)

  // Fetch the first page of orders when the dashboard mounts.
  useEffect(() => {
    dispatch(fetchOrders({ page: 0, size: 50 }))
  }, [dispatch])

  /**
   * Derive per-status counts by filtering the loaded order list.
   * Computed on each render from the Redux slice — no additional API call needed.
   */
  const countByStatus = STATUS_ORDER.reduce(
    (acc, s) => {
      acc[s] = items.filter((o) => o.status === s).length
      return acc
    },
    {} as Record<OrderStatus, number>
  )

  return (
    <div className="container" style={{ paddingTop: '32px' }}>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p style={{ color: '#888', marginTop: '4px' }}>
            Welcome back, {email}
          </p>
        </div>
        <Link to="/orders/new" className="btn btn-primary">
          + New Order
        </Link>
      </div>

      <div className="stats-grid">
        {STATUS_ORDER.map((status) => (
          <div key={status} className="stat-card">
            <div className="stat-value">{countByStatus[status]}</div>
            <div className="stat-label">{status.replace('_', ' ')}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px' }}>
        <div className="card">
          <h3 style={{ marginBottom: '16px' }}>Recent Orders</h3>
          {loading ? (
            <div className="loading">Loading orders...</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Destination</th>
                  <th>Status</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {items.slice(0, 8).map((order) => (
                  <tr key={order.id}>
                    <td>
                      <Link
                        to="/orders"
                        style={{ color: '#1890ff', textDecoration: 'none' }}
                      >
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td>{order.destinationAddress || '—'}</td>
                    <td>
                      <span className={`status-badge status-${order.status}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>
                      {new Date(order.updatedAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr>
                    <td
                      colSpan={4}
                      style={{ textAlign: 'center', color: '#888' }}
                    >
                      No orders yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: '16px' }}>
            Live Notifications{' '}
            {notifications.length > 0 && (
              <span className="live-badge">LIVE</span>
            )}
          </h3>
          <div className="notification-list">
            {notifications.length === 0 ? (
              <p style={{ color: '#888', fontSize: '13px' }}>
                No live updates yet. Updates appear here as orders change status.
              </p>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className="notification-item">
                  <div>{n.message}</div>
                  <div className="notification-time">
                    {new Date(n.timestamp).toLocaleTimeString()}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
