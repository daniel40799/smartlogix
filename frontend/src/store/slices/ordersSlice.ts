import { createAsyncThunk, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { getOrders, createOrder as createOrderApi } from '../../api/ordersApi'
import type { Order, OrdersState, OrderStatus } from '../../types'
import type { CreateOrderRequest } from '../../api/ordersApi'

const initialState: OrdersState = {
  items: [],
  loading: false,
  error: null,
  totalPages: 0,
  currentPage: 0,
}

export const fetchOrders = createAsyncThunk(
  'orders/fetchAll',
  async ({ page, size }: { page: number; size: number }) => {
    const response = await getOrders(page, size)
    return response.data
  }
)

export const createOrderThunk = createAsyncThunk(
  'orders/create',
  async (data: CreateOrderRequest) => {
    const response = await createOrderApi(data)
    return response.data
  }
)

const ordersSlice = createSlice({
  name: 'orders',
  initialState,
  reducers: {
    updateOrderStatus: (
      state,
      action: PayloadAction<{ orderId: string; newStatus: OrderStatus }>
    ) => {
      const order = state.items.find((o) => o.id === action.payload.orderId)
      if (order) {
        order.status = action.payload.newStatus
      }
    },
    addOrUpdateOrder: (state, action: PayloadAction<Order>) => {
      const idx = state.items.findIndex((o) => o.id === action.payload.id)
      if (idx >= 0) {
        state.items[idx] = action.payload
      } else {
        state.items.unshift(action.payload)
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchOrders.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchOrders.fulfilled, (state, action) => {
        state.loading = false
        state.items = action.payload.content
        state.totalPages = action.payload.totalPages
        state.currentPage = action.payload.number
      })
      .addCase(fetchOrders.rejected, (state, action) => {
        state.loading = false
        state.error = action.error.message ?? 'Failed to fetch orders'
      })
      .addCase(createOrderThunk.fulfilled, (state, action) => {
        state.items.unshift(action.payload)
      })
  },
})

export const { updateOrderStatus, addOrUpdateOrder } = ordersSlice.actions
export default ordersSlice.reducer
