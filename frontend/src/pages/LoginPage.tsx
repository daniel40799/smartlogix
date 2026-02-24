import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAppDispatch } from '../store/hooks'
import { loginThunk } from '../store/slices/authSlice'
/**
 * Login page component.
 *
 * Renders a centred card with an email/password form. On successful submission the
 * {@link loginThunk} is dispatched, which calls {@code POST /api/auth/login}, stores
 * the returned JWT in Redux state and {@code localStorage}, and navigates to the
 * Dashboard. On failure an inline error message is displayed.
 */
const LoginPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  /** Error message shown below the form when login fails. Empty string means no error. */
  const [error, setError] = useState('')
  /** {@code true} while the login API call is in flight; disables the submit button. */
  const [loading, setLoading] = useState(false)
  /**
   * Handles form submission — dispatches the login thunk and navigates on success.
   *
   * @param e - The form submit event (prevented from triggering a full-page reload).
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await dispatch(loginThunk({ email, password })).unwrap()
      navigate('/')
    } catch {
      setError('Invalid credentials. Please try again.')
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
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        <h2 style={{ marginBottom: '24px', textAlign: 'center' }}>
          🚚 SmartLogix Login
        </h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              required
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '8px' }}
            disabled={loading}
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px' }}>
          No account?{' '}
          <Link to="/register" style={{ color: '#1890ff' }}>
            Register your company
          </Link>
        </p>
      </div>
    </div>
  )
}
export default LoginPage

