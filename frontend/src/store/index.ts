import { configureStore } from '@reduxjs/toolkit'
import authReducer from './slices/authSlice'
import ordersReducer from './slices/ordersSlice'
import notificationsReducer from './slices/notificationsSlice'

/**
 * The centralised Redux store for SmartLogix.
 *
 * Combines three feature slices:
 * - {@code auth} — JWT token, user info, and authentication state.
 * - {@code orders} — paginated order list, loading/error state.
 * - {@code notifications} — real-time WebSocket order event notifications.
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    orders: ordersReducer,
    notifications: notificationsReducer,
  },
})

/** Inferred type of the entire Redux state tree. Used with {@link useAppSelector}. */
export type RootState = ReturnType<typeof store.getState>

/** Inferred type of the store's {@code dispatch} function. Used with {@link useAppDispatch}. */
export type AppDispatch = typeof store.dispatch
