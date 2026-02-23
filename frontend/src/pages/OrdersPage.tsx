import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { fetchOrders } from '../store/slices/ordersSlice'
import { transitionOrderStatus } from '../api/ordersApi'
import type { OrderStatus } from '../types'

const NEXT_STATUSES: Partial<Record<OrderStatus, OrderStatus[]>> = {
  PENDING: ['APPROVED', 'CANCELLED'],
  APPROVED: ['IN_TRANSIT', 'CANCELLED'],
  IN_TRANSIT: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
}

const OrdersPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const { items, loading, totalPages, currentPage } = useAppSelector(
    (state) => state.orders
  )
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  useEffect(() => {
    dispatch(fetchOrders({ page: 0, size: 20 }))
  }, [dispatch])

  const handleTransition = async (
    orderId: string,
    newStatus: OrderStatus
  ) => {
    setActionLoading(orderId)
    try {
      await transitionOrderStatus(orderId, newStatus)
      dispatch(fetchOrders({ page: currentPage, size: 20 }))
    } catch (err) {
      console.error('Transition failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handlePageChange = (page: number) => {
    dispatch(fetchOrders({ page, size: 20 }))
  }

  return (
    <div className="container" style={{ paddingTop: '32px' }}>
      <div className="page-header">
        <h1>Orders</h1>
        <Link to="/orders/new" className="btn btn-primary">
          + New Order
        </Link>
      </div>

      <div className="card">
        {loading ? (
          <div className="loading">Loading orders...</div>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Description</th>
                  <th>Destination</th>
                  <th>Weight (kg)</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <strong>{order.orderNumber}</strong>
                    </td>
                    <td>{order.description || '—'}</td>
                    <td>{order.destinationAddress || '—'}</td>
                    <td>{order.weight ?? '—'}</td>
                    <td>
                      <span className={`status-badge status-${order.status}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                        {(NEXT_STATUSES[order.status] ?? []).map((next) => (
                          <button
                            key={next}
                            className={`btn ${next === 'CANCELLED' ? 'btn-danger' : 'btn-secondary'}`}
                            style={{ fontSize: '12px', padding: '4px 8px' }}
                            onClick={() => handleTransition(order.id, next)}
                            disabled={actionLoading === order.id}
                          >
                            {next === 'CANCELLED' ? 'Cancel' : `→ ${next}`}
                          </button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr>
                    <td
                      colSpan={7}
                      style={{ textAlign: 'center', color: '#888' }}
                    >
                      No orders found.{' '}
                      <Link to="/orders/new" style={{ color: '#1890ff' }}>
                        Create your first order
                      </Link>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
            {totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'center',
                  gap: '8px',
                  marginTop: '16px',
                }}
              >
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    className={`btn ${i === currentPage ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ minWidth: '36px' }}
                    onClick={() => handlePageChange(i)}
                  >
                    {i + 1}
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default OrdersPage
