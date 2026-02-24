import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAppDispatch } from '../store/hooks'
import { registerThunk } from '../store/slices/authSlice'

/**
 * Registration page component.
 *
 * Allows a new company (tenant) to sign up by providing a unique slug, an admin email,
 * and a password. On success the {@link registerThunk} is dispatched, the returned JWT
 * is persisted, and the user is redirected to the Dashboard as the first admin user of
 * the newly created tenant.
 */
const RegisterPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  /** Controlled form state for all registration fields. */
  const [form, setForm] = useState({
    email: '',
    password: '',
    tenantSlug: '',
  })
  /** Error message shown when registration fails. Empty string means no error. */
  const [error, setError] = useState('')
  /** {@code true} while the registration API call is in flight. */
  const [loading, setLoading] = useState(false)

  /**
   * Generic change handler that updates the matching field in {@link form} state.
   *
   * @param e - Input change event.
   */
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  /**
   * Handles form submission — dispatches the registration thunk and navigates on success.
   *
   * @param e - The form submit event.
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await dispatch(registerThunk(form)).unwrap()
      navigate('/')
    } catch {
      setError('Registration failed. Slug may already be taken.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
      }}
    >
      <div className="card" style={{ width: '100%', maxWidth: '420px' }}>
        <h2 style={{ marginBottom: '24px', textAlign: 'center' }}>
          🏢 Register Your Company
        </h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Company Slug (unique identifier)</label>
            <input
              name="tenantSlug"
              value={form.tenantSlug}
              onChange={handleChange}
              placeholder="my-logistics-company"
              required
            />
          </div>
          <div className="form-group">
            <label>Admin Email</label>
            <input
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              placeholder="admin@company.com"
              required
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Minimum 8 characters"
              required
              minLength={8}
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '8px' }}
            disabled={loading}
          >
            {loading ? 'Creating account...' : 'Register & Get Started'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: '#1890ff' }}>
            Login
          </Link>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
