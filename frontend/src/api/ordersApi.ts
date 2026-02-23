import axiosInstance from './axiosInstance'
import { Order, OrderStatus, PageResponse } from '../types'

export interface CreateOrderRequest {
  orderNumber: string
  description: string
  destinationAddress: string
  weight: number
  latitude?: number
  longitude?: number
  trackingNotes?: string
}

export const getOrders = (page = 0, size = 10) =>
  axiosInstance.get<PageResponse<Order>>(`/orders?page=${page}&size=${size}`)

export const createOrder = (data: CreateOrderRequest) =>
  axiosInstance.post<Order>('/orders', data)

export const getOrderById = (id: string) =>
  axiosInstance.get<Order>(`/orders/${id}`)

export const transitionOrderStatus = (id: string, newStatus: OrderStatus) =>
  axiosInstance.patch<Order>(`/orders/${id}/status`, { newStatus })
