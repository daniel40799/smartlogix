/**
 * All possible lifecycle states of an order in the SmartLogix state machine.
 * Mirrors the backend {@code OrderStatus} enum.
 */
export type OrderStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'

/**
 * Represents a single logistics order as returned by the SmartLogix REST API.
 */
export interface Order {
  /** UUID of the order record. */
  id: string
  /** Human-readable, unique order identifier (e.g. {@code ORD-2024-001}). */
  orderNumber: string
  /** Free-text description of the shipment contents. */
  description: string
  /** Current lifecycle status of the order. */
  status: OrderStatus
  /** Full delivery address string. */
  destinationAddress: string
  /** Gross weight of the shipment in kilograms. */
  weight: number
  /** WGS-84 latitude for map display; {@code null} if not provided. */
  latitude: number | null
  /** WGS-84 longitude for map display; {@code null} if not provided. */
  longitude: number | null
  /** Additional tracking or handling notes; {@code null} if not provided. */
  trackingNotes: string | null
  /** UUID of the tenant that owns this order. */
  tenantId: string
  /** ISO-8601 UTC timestamp of when the order was first created. */
  createdAt: string
  /** ISO-8601 UTC timestamp of the most recent modification. */
  updatedAt: string
}

/**
 * Redux slice state shape for authentication.
 * Persisted in {@code localStorage} so the user remains logged in after a page refresh.
 */
export interface AuthState {
  /** Signed JWT token; {@code null} when not authenticated. */
  token: string | null
  /** Email of the authenticated user; {@code null} when not authenticated. */
  email: string | null
  /** UUID of the tenant the authenticated user belongs to. */
  tenantId: string | null
  /** Role string of the authenticated user (e.g. {@code ROLE_USER}). */
  role: string | null
  /** Convenience flag derived from {@link token}; {@code true} when a token is present. */
  isAuthenticated: boolean
}

/**
 * A single real-time order status notification received over the WebSocket connection.
 */
export interface Notification {
  /** Unique composite identifier: {@code "{orderId}-{timestamp}"}. */
  id: string
  /** Human-readable notification message shown in the Live Notifications panel. */
  message: string
  /** ISO-8601 UTC timestamp of when the notification was emitted. */
  timestamp: string
  /** UUID of the order associated with this notification. */
  orderId: string
  /** Event type string from the backend (e.g. {@code "OrderStatusChanged"}). */
  eventType: string
}

/**
 * Redux slice state shape for the orders list.
 */
export interface OrdersState {
  /** Paginated list of orders currently loaded in the UI. */
  items: Order[]
  /** {@code true} while an async fetch is in progress. */
  loading: boolean
  /** Error message from the last failed fetch; {@code null} if no error. */
  error: string | null
  /** Total number of pages returned by the last paginated API call. */
  totalPages: number
  /** Zero-based index of the page currently displayed. */
  currentPage: number
}

/**
 * Generic wrapper for Spring Data's paginated API responses.
 *
 * @template T The type of items in the page content array.
 */
export interface PageResponse<T> {
  /** Array of items on the current page. */
  content: T[]
  /** Total number of pages available. */
  totalPages: number
  /** Total number of items across all pages. */
  totalElements: number
  /** Zero-based index of the current page. */
  number: number
}
