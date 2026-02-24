import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch } from '../store/hooks'
import { createOrderThunk } from '../store/slices/ordersSlice'

/**
 * Create Order page component.
 *
 * Renders a form for creating a new logistics order. Required fields are
 * {@code orderNumber}, {@code destinationAddress}, and {@code weight}; all other
 * fields ({@code description}, {@code latitude}, {@code longitude}, {@code trackingNotes})
 * are optional. On success the order is added to the Redux store and the user is
 * redirected to the Orders list.
 */
const CreateOrderPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  /** Controlled form state for all order fields. */
  const [form, setForm] = useState({
    orderNumber: '',
    description: '',
    destinationAddress: '',
    weight: '',
    latitude: '',
    longitude: '',
    trackingNotes: '',
  })
  /** Error message shown below the form when creation fails. */
  const [error, setError] = useState('')
  /** {@code true} while the create API call is in flight; disables the submit button. */
  const [loading, setLoading] = useState(false)

  /**
   * Generic change handler that updates the matching field in {@link form} state.
   *
   * @param e - Input or textarea change event.
   */
  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  /**
   * Handles form submission — builds the typed payload, dispatches {@link createOrderThunk},
   * and navigates to {@code /orders} on success.
   *
   * @param e - The form submit event.
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await dispatch(
        createOrderThunk({
          orderNumber: form.orderNumber,
          description: form.description,
          destinationAddress: form.destinationAddress,
          weight: parseFloat(form.weight) || 0,
          latitude: form.latitude ? parseFloat(form.latitude) : undefined,
          longitude: form.longitude ? parseFloat(form.longitude) : undefined,
          trackingNotes: form.trackingNotes || undefined,
        })
      ).unwrap()
      navigate('/orders')
    } catch {
      setError('Failed to create order. Please check the form and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="container"
      style={{ paddingTop: '32px', maxWidth: '600px' }}
    >
      <div className="page-header">
        <h1>Create New Order</h1>
      </div>
      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Order Number *</label>
            <input
              name="orderNumber"
              value={form.orderNumber}
              onChange={handleChange}
              placeholder="ORD-2024-001"
              required
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              placeholder="Brief description of the shipment"
              rows={3}
              style={{ resize: 'vertical' }}
            />
          </div>
          <div className="form-group">
            <label>Destination Address *</label>
            <input
              name="destinationAddress"
              value={form.destinationAddress}
              onChange={handleChange}
              placeholder="123 Main St, City, Country"
              required
            />
          </div>
          <div className="form-group">
            <label>Weight (kg) *</label>
            <input
              name="weight"
              type="number"
              step="0.01"
              min="0"
              value={form.weight}
              onChange={handleChange}
              placeholder="0.00"
              required
            />
          </div>
          <div
            style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}
          >
            <div className="form-group">
              <label>Latitude</label>
              <input
                name="latitude"
                type="number"
                step="any"
                value={form.latitude}
                onChange={handleChange}
                placeholder="e.g. 40.7128"
              />
            </div>
            <div className="form-group">
              <label>Longitude</label>
              <input
                name="longitude"
                type="number"
                step="any"
                value={form.longitude}
                onChange={handleChange}
                placeholder="e.g. -74.0060"
              />
            </div>
          </div>
          <div className="form-group">
            <label>Tracking Notes</label>
            <textarea
              name="trackingNotes"
              value={form.trackingNotes}
              onChange={handleChange}
              placeholder="Additional tracking information"
              rows={2}
              style={{ resize: 'vertical' }}
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create Order'}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate('/orders')}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CreateOrderPage
