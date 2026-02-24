import React, { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { fetchOrders } from '../store/slices/ordersSlice'

// Fix leaflet default marker icons broken by Webpack/Vite asset bundling:
// the bundler renames the image files so leaflet's built-in URL resolution fails.
// Deleting the private resolver and merging explicit CDN URLs restores the icons.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
})

/**
 * Map page component — visualises orders with GPS coordinates on an interactive world map.
 *
 * Uses React Leaflet with an OpenStreetMap tile layer. Each order that has both
 * {@code latitude} and {@code longitude} fields set is rendered as a map marker.
 * Clicking a marker opens a popup showing the order number, status badge, and destination
 * address.
 *
 * Up to 100 orders are fetched on mount. Orders without coordinates are excluded from
 * the map but still counted in the header subtitle.
 */
const MapPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const { items, loading } = useAppSelector((state) => state.orders)

  // Fetch a large page of orders so the map displays as many pins as possible.
  useEffect(() => {
    dispatch(fetchOrders({ page: 0, size: 100 }))
  }, [dispatch])

  /** Subset of orders that have valid latitude and longitude values. */
  const ordersWithCoords = items.filter(
    (o) => o.latitude != null && o.longitude != null
  )

  return (
    <div className="container" style={{ paddingTop: '32px' }}>
      <div className="page-header">
        <h1>Shipment Map</h1>
        <span style={{ color: '#888', fontSize: '14px' }}>
          {ordersWithCoords.length} shipment(s) with coordinates
        </span>
      </div>
      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div className="loading" style={{ padding: '40px' }}>
            Loading map...
          </div>
        ) : (
          <div className="map-container">
            <MapContainer
              center={[20, 0]}
              zoom={2}
              style={{ height: '100%', width: '100%' }}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {ordersWithCoords.map((order) => (
                <Marker
                  key={order.id}
                  position={[order.latitude!, order.longitude!]}
                >
                  <Popup>
                    <strong>{order.orderNumber}</strong>
                    <br />
                    <span
                      className={`status-badge status-${order.status}`}
                    >
                      {order.status}
                    </span>
                    <br />
                    {order.destinationAddress}
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
          </div>
        )}
      </div>
      {ordersWithCoords.length === 0 && !loading && (
        <div className="card" style={{ textAlign: 'center', color: '#888' }}>
          No orders with GPS coordinates found. Add latitude/longitude when
          creating orders to see them on the map.
        </div>
      )}
    </div>
  )
}

export default MapPage
