import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { Notification } from '../../types'

interface NotificationsState {
  items: Notification[]
}

const initialState: NotificationsState = {
  items: [],
}

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    addNotification: (state, action: PayloadAction<Notification>) => {
      state.items.unshift(action.payload)
      if (state.items.length > 50) {
        state.items.pop()
      }
    },
    clearNotifications: (state) => {
      state.items = []
    },
  },
})

export const { addNotification, clearNotifications } =
  notificationsSlice.actions
export default notificationsSlice.reducer
