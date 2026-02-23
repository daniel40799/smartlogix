import axiosInstance from './axiosInstance'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  tenantSlug: string
}

export interface AuthResponse {
  token: string
  email: string
  tenantId: string
  role: string
}

export const login = (data: LoginRequest) =>
  axiosInstance.post<AuthResponse>('/auth/login', data)

export const register = (data: RegisterRequest) =>
  axiosInstance.post<AuthResponse>('/auth/register', data)
