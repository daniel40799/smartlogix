import { useEffect, useRef, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { addNotification } from '../store/slices/notificationsSlice'
import { updateOrderStatus } from '../store/slices/ordersSlice'
import type { OrderStatus } from '../types'

/**
 * Shape of the order domain event payload received over WebSocket.
 * Mirrors the backend {@code OrderEvent} class.
 */
interface OrderEvent {
  /** Discriminator string (e.g. {@code "OrderCreated"}, {@code "OrderStatusChanged"}). */
  eventType: string
  /** UUID of the affected order. */
  orderId: string
  /** UUID of the owning tenant. */
  tenantId: string
  /** Current status of the order at the time the event was emitted. */
  status: OrderStatus
  /** ISO-8601 UTC timestamp of when the event was created by the backend. */
  timestamp: string
}

/**
 * Custom React hook that manages a persistent STOMP-over-WebSocket connection to the
 * SmartLogix backend.
 *
 * On mount (when the user is authenticated) the hook:
 * 1. Creates a STOMP client using SockJS as the transport layer.
 * 2. Subscribes to the tenant-scoped topic {@code /topic/orders/{tenantId}}.
 * 3. Dispatches {@link addNotification} and {@link updateOrderStatus} for each received event
 *    so the Notifications panel and the Orders table update in real time.
 * 4. Automatically reconnects every 5 seconds if the connection is lost.
 *
 * On unmount the client is deactivated to release the underlying WebSocket connection.
 *
 * @returns An object with {@code isConnected} indicating whether the STOMP client is
 *          currently connected.
 */
export const useWebSocket = () => {
  const dispatch = useAppDispatch()
  const { token, tenantId, isAuthenticated } = useAppSelector(
    (state) => state.auth
  )
  /** Stable ref holding the STOMP {@link Client} instance across renders. */
  const clientRef = useRef<Client | null>(null)

  /**
   * Creates and activates the STOMP client.
   * Wrapped in {@code useCallback} so the reference is stable and only changes when
   * authentication state changes, preventing duplicate connections.
   */
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
      /** Reconnect delay in milliseconds. */
      reconnectDelay: 5000,
    })

    client.activate()
    clientRef.current = client
  }, [dispatch, isAuthenticated, tenantId, token])

  useEffect(() => {
    connect()
    return () => {
      // Deactivate the STOMP client when the component using this hook unmounts.
      clientRef.current?.deactivate()
    }
  }, [connect])

  return { isConnected: !!clientRef.current?.connected }
}
