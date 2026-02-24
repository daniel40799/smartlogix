import { createAsyncThunk, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { getOrders, createOrder as createOrderApi } from '../../api/ordersApi'
import type { Order, OrdersState, OrderStatus } from '../../types'
import type { CreateOrderRequest } from '../../api/ordersApi'

/** Initial state for the orders slice — empty list, not loading, no error. */
const initialState: OrdersState = {
  items: [],
  loading: false,
  error: null,
  totalPages: 0,
  currentPage: 0,
}

/**
 * Async thunk that fetches a paginated list of orders from the backend.
 * Calls {@code GET /api/orders?page={page}&size={size}}.
 */
export const fetchOrders = createAsyncThunk(
  'orders/fetchAll',
  async ({ page, size }: { page: number; size: number }) => {
    const response = await getOrders(page, size)
    return response.data
  }
)

/**
 * Async thunk that creates a new order via the REST API.
 * Calls {@code POST /api/orders} and prepends the created order to the Redux list.
 */
export const createOrderThunk = createAsyncThunk(
  'orders/create',
  async (data: CreateOrderRequest) => {
    const response = await createOrderApi(data)
    return response.data
  }
)

/**
 * Redux slice managing the orders list, pagination, and loading state.
 *
 * Synchronous reducers:
 * - {@code updateOrderStatus} — optimistically updates a single order's status in the list,
 *   used when a WebSocket event arrives.
 * - {@code addOrUpdateOrder} — upserts a full order object (replaces if found, prepends if new).
 *
 * Async extra reducers handle the pending/fulfilled/rejected lifecycle of {@link fetchOrders}
 * and the fulfilled case of {@link createOrderThunk}.
 */
const ordersSlice = createSlice({
  name: 'orders',
  initialState,
  reducers: {
    /**
     * Updates the status of an order already present in the list.
     * Triggered by real-time WebSocket events so the UI reflects status changes without
     * requiring a full page refresh or re-fetch.
     *
     * @param action.payload.orderId   - UUID of the order to update.
     * @param action.payload.newStatus - The new status value to apply.
     */
    updateOrderStatus: (
      state,
      action: PayloadAction<{ orderId: string; newStatus: OrderStatus }>
    ) => {
      const order = state.items.find((o) => o.id === action.payload.orderId)
      if (order) {
        order.status = action.payload.newStatus
      }
    },
    /**
     * Inserts or replaces an order in the list.
     * If an order with the same ID already exists it is replaced in-place; otherwise the
     * new order is prepended so it appears at the top of the list.
     *
     * @param action.payload - The full {@link Order} object to upsert.
     */
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
      // Set loading flag while the fetch is in progress.
      .addCase(fetchOrders.pending, (state) => {
        state.loading = true
        state.error = null
      })
      // Populate the list and pagination metadata when the fetch completes.
      .addCase(fetchOrders.fulfilled, (state, action) => {
        state.loading = false
        state.items = action.payload.content
        state.totalPages = action.payload.totalPages
        state.currentPage = action.payload.number
      })
      // Store the error message so the UI can display it.
      .addCase(fetchOrders.rejected, (state, action) => {
        state.loading = false
        state.error = action.error.message ?? 'Failed to fetch orders'
      })
      // Prepend the newly created order to the top of the list.
      .addCase(createOrderThunk.fulfilled, (state, action) => {
        state.items.unshift(action.payload)
      })
  },
})

export const { updateOrderStatus, addOrUpdateOrder } = ordersSlice.actions
export default ordersSlice.reducer
