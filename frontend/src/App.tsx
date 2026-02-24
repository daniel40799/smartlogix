import React, { lazy, memo, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAppSelector } from './store/hooks'
import { useWebSocket } from './hooks/useWebSocket'
import Navbar from './components/Navbar'

// Lazy-load page components so each page's JS bundle is loaded on demand,
// reducing the initial bundle size and improving first-paint performance.
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const CreateOrderPage = lazy(() => import('./pages/CreateOrderPage'))
const MapPage = lazy(() => import('./pages/MapPage'))

/**
 * Route guard component that redirects unauthenticated users to {@code /login}.
 *
 * @param children - The protected page content to render when authenticated.
 */
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = memo(({
  children,
}) => {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
})

/**
 * Fallback UI shown by {@link Suspense} while a lazy-loaded page chunk is being fetched.
 */
const PageFallback: React.FC = memo(() => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
    <div className="loading">Loading...</div>
  </div>
))

/**
 * Inner application component that activates the WebSocket connection and renders the
 * full page routing tree.
 *
 * Separated from {@link App} so the {@link useWebSocket} hook is mounted once at the
 * application level and remains active for the entire authenticated session.
 */
const AppContent: React.FC = memo(() => {
  useWebSocket()
  return (
    <>
      <Navbar />
      <Suspense fallback={<PageFallback />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrdersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders/new"
            element={
              <ProtectedRoute>
                <CreateOrderPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/map"
            element={
              <ProtectedRoute>
                <MapPage />
              </ProtectedRoute>
            }
          />
          {/* Catch-all: redirect any unknown path to the dashboard. */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </>
  )
})

/**
 * Root application component.
 * Renders {@link AppContent} which contains the full routing and WebSocket setup.
 */
const App: React.FC = () => {
  return <AppContent />
}

export default App
