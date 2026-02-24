import axiosInstance from './axiosInstance'

/** Request body for {@code POST /api/auth/login}. */
export interface LoginRequest {
  /** The user's email address. */
  email: string
  /** The user's plain-text password. */
  password: string
}

/** Request body for {@code POST /api/auth/register}. */
export interface RegisterRequest {
  /** Email address for the new user account. */
  email: string
  /** Plain-text password (must be at least 8 characters). */
  password: string
  /** URL-friendly slug identifying the tenant (company) to register under. */
  tenantSlug: string
}

/** Response body returned by both the login and register endpoints. */
export interface AuthResponse {
  /** Signed JWT to use as a {@code Bearer} token in subsequent requests. */
  token: string
  /** Email of the authenticated user. */
  email: string
  /** UUID of the tenant the user belongs to. */
  tenantId: string
  /** Role string of the authenticated user (e.g. {@code ROLE_USER}). */
  role: string
}

/**
 * Authenticates a user with email and password.
 * Calls {@code POST /api/auth/login} and returns an {@link AuthResponse} on success.
 *
 * @param data - Login credentials.
 */
export const login = (data: LoginRequest) =>
  axiosInstance.post<AuthResponse>('/auth/login', data)

/**
 * Registers a new user, creating the tenant if it does not already exist.
 * Calls {@code POST /api/auth/register} and returns an {@link AuthResponse} on success.
 *
 * @param data - Registration details including email, password, and tenant slug.
 */
export const register = (data: RegisterRequest) =>
  axiosInstance.post<AuthResponse>('/auth/register', data)
