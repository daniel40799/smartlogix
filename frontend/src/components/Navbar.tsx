import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { logout } from '../store/slices/authSlice'

/**
 * Top navigation bar rendered on every authenticated page.
 *
 * Displays:
 * - Brand link back to the Dashboard.
 * - Navigation links: Dashboard, Orders, Map View.
 * - Live notification badge showing the count of unread WebSocket events.
 * - The authenticated user's email address.
 * - A Logout button that clears auth state and redirects to {@code /login}.
 *
 * Returns {@code null} when the user is not authenticated so the bar is hidden on
 * the Login and Register pages.
 */
const Navbar: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const { isAuthenticated, email } = useAppSelector((state) => state.auth)
  /** Total count of unread live notifications; shown as a badge in the nav bar. */
  const notificationCount = useAppSelector(
    (state) => state.notifications.items.length
  )

  /** Dispatches the logout action and navigates to the login page. */
  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }

  if (!isAuthenticated) return null

  return (
    <nav className="navbar">
      <NavLink to="/" className="navbar-brand">
        🚚 SmartLogix
      </NavLink>
      <div className="navbar-links">
        <NavLink to="/">Dashboard</NavLink>
        <NavLink to="/orders">Orders</NavLink>
        <NavLink to="/map">Map View</NavLink>
      </div>
      <div className="navbar-right">
        {notificationCount > 0 && (
          <span className="live-badge">LIVE ({notificationCount})</span>
        )}
        <span style={{ color: 'rgba(255,255,255,0.65)', fontSize: '13px' }}>
          {email}
        </span>
        <button className="btn btn-secondary" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </nav>
  )
}

export default Navbar
