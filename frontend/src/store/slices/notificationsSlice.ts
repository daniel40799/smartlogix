import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { Notification } from '../../types'

/** Shape of the notifications slice state. */
interface NotificationsState {
  /** Ordered list of notifications, most recent first. Capped at 50 items. */
  items: Notification[]
}

/** Initial state — empty list. */
const initialState: NotificationsState = {
  items: [],
}

/**
 * Redux slice that manages the in-memory queue of real-time order event notifications.
 * Notifications arrive via the {@link useWebSocket} hook when the backend publishes
 * order events to the tenant's WebSocket topic.
 *
 * Reducers:
 * - {@code addNotification} — prepends a new notification and trims the list to 50 entries.
 * - {@code clearNotifications} — empties the notification list.
 */
const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    /**
     * Prepends a notification to the list.
     * If the list would exceed 50 items the oldest entry is removed to keep memory bounded.
     *
     * @param action.payload - The {@link Notification} to add.
     */
    addNotification: (state, action: PayloadAction<Notification>) => {
      state.items.unshift(action.payload)
      // Keep the list bounded to prevent unbounded memory growth in long-running sessions.
      if (state.items.length > 50) {
        state.items.pop()
      }
    },
    /**
     * Removes all notifications from the list.
     * Typically called when the user explicitly dismisses all alerts.
     */
    clearNotifications: (state) => {
      state.items = []
    },
  },
})

export const { addNotification, clearNotifications } =
  notificationsSlice.actions
export default notificationsSlice.reducer
