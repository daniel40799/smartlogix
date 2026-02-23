import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { logout } from '../store/slices/authSlice'

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const { isAuthenticated, email } = useAppSelector((state) => state.auth)
  const notificationCount = useAppSelector(
    (state) => state.notifications.items.length
  )

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
