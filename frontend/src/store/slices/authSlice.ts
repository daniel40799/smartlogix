import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { login as loginApi, register as registerApi } from '../../api/authApi'
import type { AuthState } from '../../types'

const storedToken = localStorage.getItem('token')
const storedEmail = localStorage.getItem('email')
const storedTenantId = localStorage.getItem('tenantId')
const storedRole = localStorage.getItem('role')

const initialState: AuthState = {
  token: storedToken,
  email: storedEmail,
  tenantId: storedTenantId,
  role: storedRole,
  isAuthenticated: !!storedToken,
}

export const loginThunk = createAsyncThunk(
  'auth/login',
  async (credentials: { email: string; password: string }) => {
    const response = await loginApi(credentials)
    return response.data
  }
)

export const registerThunk = createAsyncThunk(
  'auth/register',
  async (data: { email: string; password: string; tenantSlug: string }) => {
    const response = await registerApi(data)
    return response.data
  }
)

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
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
