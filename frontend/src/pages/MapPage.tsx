import React, { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { fetchOrders } from '../store/slices/ordersSlice'

// Fix leaflet default marker icons
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

const MapPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const { items, loading } = useAppSelector((state) => state.orders)

  useEffect(() => {
    dispatch(fetchOrders({ page: 0, size: 100 }))
  }, [dispatch])

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
