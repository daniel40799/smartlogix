import { useEffect, useRef, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { addNotification } from '../store/slices/notificationsSlice'
import { updateOrderStatus } from '../store/slices/ordersSlice'
import type { OrderStatus } from '../types'

interface OrderEvent {
  eventType: string
  orderId: string
  tenantId: string
  status: OrderStatus
  timestamp: string
}

export const useWebSocket = () => {
  const dispatch = useAppDispatch()
  const { token, tenantId, isAuthenticated } = useAppSelector(
    (state) => state.auth
  )
  const clientRef = useRef<Client | null>(null)

  const connect = useCallback(() => {
    if (!isAuthenticated || !tenantId || !token) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        client.subscribe(`/topic/orders/${tenantId}`, (message: IMessage) => {
          const event: OrderEvent = JSON.parse(message.body)
          dispatch(
            addNotification({
              id: `${event.orderId}-${event.timestamp}`,
              message: `Order ${event.orderId.slice(0, 8)}... → ${event.status}`,
              timestamp: event.timestamp,
              orderId: event.orderId,
              eventType: event.eventType,
            })
          )
          dispatch(
            updateOrderStatus({
              orderId: event.orderId,
              newStatus: event.status,
            })
          )
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
      },
      reconnectDelay: 5000,
    })

    client.activate()
    clientRef.current = client
  }, [dispatch, isAuthenticated, tenantId, token])

  useEffect(() => {
    connect()
    return () => {
      clientRef.current?.deactivate()
    }
  }, [connect])

  return { isConnected: !!clientRef.current?.connected }
}
