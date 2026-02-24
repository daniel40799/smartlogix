import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { login as loginApi, register as registerApi } from '../../api/authApi'
import type { AuthState } from '../../types'

// ---------------------------------------------------------------------------
// Rehydrate auth state from localStorage on page load so the user remains
// logged in after a browser refresh.
// ---------------------------------------------------------------------------
const storedToken = localStorage.getItem('token')
const storedEmail = localStorage.getItem('email')
const storedTenantId = localStorage.getItem('tenantId')
const storedRole = localStorage.getItem('role')

/** Initial auth state, populated from {@code localStorage} if a previous session exists. */
const initialState: AuthState = {
  token: storedToken,
  email: storedEmail,
  tenantId: storedTenantId,
  role: storedRole,
  isAuthenticated: !!storedToken,
}

/**
 * Async thunk for user login.
 * Calls {@code POST /api/auth/login} via {@link loginApi} and returns the server response
 * so that {@code extraReducers} can update the Redux state and persist the token.
 */
export const loginThunk = createAsyncThunk(
  'auth/login',
  async (credentials: { email: string; password: string }) => {
    const response = await loginApi(credentials)
    return response.data
  }
)

/**
 * Async thunk for user registration.
 * Calls {@code POST /api/auth/register} via {@link registerApi}. On success the user is
 * immediately authenticated — the returned JWT is stored in Redux state and localStorage.
 */
export const registerThunk = createAsyncThunk(
  'auth/register',
  async (data: { email: string; password: string; tenantSlug: string }) => {
    const response = await registerApi(data)
    return response.data
  }
)

/**
 * Redux slice managing authentication state.
 *
 * Reducers:
 * - {@code logout} — clears all auth state and removes tokens from {@code localStorage}.
 *
 * Extra reducers handle the fulfilled cases of {@link loginThunk} and {@link registerThunk}
 * by storing the JWT and user metadata in both Redux state and {@code localStorage}.
 */
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /**
     * Clears the authentication state and removes all auth-related items from
     * {@code localStorage}. Called on explicit user logout.
     */
    logout: (state) => {
      state.token = null
      state.email = null
      state.tenantId = null
      state.role = null
      state.isAuthenticated = false
      localStorage.removeItem('token')
      localStorage.removeItem('email')
      localStorage.removeItem('tenantId')
      localStorage.removeItem('role')
    },
  },
  extraReducers: (builder) => {
    builder
      // Persist auth data when login succeeds.
      .addCase(loginThunk.fulfilled, (state, action) => {
        state.token = action.payload.token
        state.email = action.payload.email
        state.tenantId = action.payload.tenantId
        state.role = action.payload.role
        state.isAuthenticated = true
        localStorage.setItem('token', action.payload.token)
        localStorage.setItem('email', action.payload.email)
        localStorage.setItem('tenantId', action.payload.tenantId)
        localStorage.setItem('role', action.payload.role)
      })
      // Persist auth data when registration succeeds (user is immediately logged in).
      .addCase(registerThunk.fulfilled, (state, action) => {
        state.token = action.payload.token
        state.email = action.payload.email
        state.tenantId = action.payload.tenantId
        state.role = action.payload.role
        state.isAuthenticated = true
        localStorage.setItem('token', action.payload.token)
        localStorage.setItem('email', action.payload.email)
        localStorage.setItem('tenantId', action.payload.tenantId)
        localStorage.setItem('role', action.payload.role)
      })
  },
})

export const { logout } = authSlice.actions
export default authSlice.reducer
