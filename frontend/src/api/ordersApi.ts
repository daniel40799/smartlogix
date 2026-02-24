import axiosInstance from './axiosInstance'
import { Order, OrderStatus, PageResponse } from '../types'

/** Request body for {@code POST /api/orders}. */
export interface CreateOrderRequest {
  /** Unique human-readable order identifier (e.g. {@code ORD-2024-001}). Required. */
  orderNumber: string
  /** Free-text description of the shipment contents. */
  description: string
  /** Full delivery address string. */
  destinationAddress: string
  /** Gross weight of the shipment in kilograms. */
  weight: number
  /** Optional WGS-84 latitude for map display. */
  latitude?: number
  /** Optional WGS-84 longitude for map display. */
  longitude?: number
  /** Optional free-text tracking or handling notes. */
  trackingNotes?: string
}

/**
 * Fetches a paginated list of orders for the current tenant.
 * Calls {@code GET /api/orders?page={page}&size={size}}.
 *
 * @param page - Zero-based page index (default {@code 0}).
 * @param size - Number of records per page (default {@code 10}).
 */
export const getOrders = (page = 0, size = 10) =>
  axiosInstance.get<PageResponse<Order>>(`/orders?page=${page}&size=${size}`)

/**
 * Creates a new order for the current tenant.
 * Calls {@code POST /api/orders}.
 *
 * @param data - Order creation payload.
 */
export const createOrder = (data: CreateOrderRequest) =>
  axiosInstance.post<Order>('/orders', data)

/**
 * Retrieves a single order by its UUID.
 * Calls {@code GET /api/orders/{id}}.
 *
 * @param id - UUID of the order to fetch.
 */
export const getOrderById = (id: string) =>
  axiosInstance.get<Order>(`/orders/${id}`)

/**
 * Transitions an order to a new status following the backend state-machine rules.
 * Calls {@code PATCH /api/orders/{id}/status}.
 *
 * @param id        - UUID of the order to update.
 * @param newStatus - The target status to transition the order to.
 */
export const transitionOrderStatus = (id: string, newStatus: OrderStatus) =>
  axiosInstance.patch<Order>(`/orders/${id}/status`, { newStatus })
