export type OrderStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'

export interface Order {
  id: string
  orderNumber: string
  description: string
  status: OrderStatus
  destinationAddress: string
  weight: number
  latitude: number | null
  longitude: number | null
  trackingNotes: string | null
  tenantId: string
  createdAt: string
  updatedAt: string
}

export interface AuthState {
  token: string | null
  email: string | null
  tenantId: string | null
  role: string | null
  isAuthenticated: boolean
}

export interface Notification {
  id: string
  message: string
  timestamp: string
  orderId: string
  eventType: string
}

export interface OrdersState {
  items: Order[]
  loading: boolean
  error: string | null
  totalPages: number
  currentPage: number
}

export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
}
