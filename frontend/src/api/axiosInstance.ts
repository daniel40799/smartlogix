import axios from 'axios'

/**
 * Pre-configured Axios instance used by all SmartLogix API modules.
 *
 * - Base URL is set to {@code /api} so that the Vite dev-server proxy (and the Nginx
 *   reverse proxy in production) routes requests to the Spring Boot backend.
 * - Default {@code Content-Type} header is set to {@code application/json}.
 */
const axiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * Request interceptor — attaches the JWT token from {@code localStorage} as a
 * {@code Authorization: Bearer <token>} header on every outgoing request, so the
 * Spring Security filter chain can authenticate the caller.
 */
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Response interceptor — handles authentication errors globally.
 * When the server responds with {@code 401 Unauthorized} or {@code 403 Forbidden}
 * the stored token is removed from {@code localStorage} and the user is redirected
 * to the login page, preventing stale or revoked tokens from lingering in the UI.
 */
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default axiosInstance
