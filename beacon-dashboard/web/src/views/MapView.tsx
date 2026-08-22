import { MapContainer, TileLayer, Marker, Popup, CircleMarker } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useBeaconStore } from '../stores/beaconStore'
import { MapPin, Users, Home, Droplet, Heart, AlertTriangle } from 'lucide-react'

const resourceIcons = {
  WATER: { icon: Droplet, color: '#3B82F6' },
  FOOD: { icon: Heart, color: '#22C55E' },
  MEDICAL: { icon: Heart, color: '#EF4444' },
  SHELTER: { icon: Home, color: '#8B5CF6' },
  CHARGING: { icon: Heart, color: '#F59E0B' },
  HAZARD: { icon: AlertTriangle, color: '#EF4444' },
  ROAD_CLOSED: { icon: AlertTriangle, color: '#F97316' },
}

export function MapView() {
  const { 
    node, 
    peers, 
    resources, 
    mapCenter, 
    mapZoom, 
    setMapView,
    selectedResource,
    setSelectedResource 
  } = useBeaconStore()

  // Default to a reasonable location if node not set
  const center = node?.id ? [0, 0] : [mapCenter.lat, mapCenter.lng]

  return (
    <div className="h-full w-full">
      <MapContainer
        center={center}
        zoom={mapZoom}
        onMoveend={(e) => {
          const center = e.target.getCenter()
          const zoom = e.target.getZoom()
          setMapView({ lat: center.lat, lng: center.lng }, zoom)
        }}
        className="h-full w-full"
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        
        {/* Current node location */}
        {node && (
          <Marker position={[0, 0]} icon={null}>
            <div className="w-4 h-4 bg-primary rounded-full border-2 border-white shadow-lg animate-pulse" />
          </Marker>
        )}
        
        {/* Peer markers */}
        {peers
          .filter(p => p.location && p.isOnline)
          .map((peer) => (
            <Marker key={peer.id} position={[peer.location!.lat, peer.location!.lng]}>
              <div className="flex flex-col items-center">
                <div className="w-3 h-3 bg-blue-500 rounded-full border-2 border-white shadow-lg" />
                <span className="text-xs bg-white/90 px-1 rounded shadow">Peer</span>
              </div>
              <Popup>
                <div className="p-2 min-w-[150px]">
                  <p className="font-medium">{peer.displayName || 'Unknown Peer'}</p>
                  <p className="text-sm text-text-secondary">
                    {peer.batteryLevel !== undefined ? `Battery: ${peer.batteryLevel}%` : ''}
                    {peer.signalStrength !== undefined ? ` • RSSI: ${peer.signalStrength}dBm` : ''}
                  </p>
                  <p className="text-xs text-text-secondary mt-1">
                    Last seen: {new Date(peer.lastSeen).toLocaleTimeString()}
                  </p>
                </div>
              </Popup>
            </Marker>
          ))}
        )}
        
        {/* Resource markers */}
        {resources.map((resource) => {
          const { icon: Icon, color } = resourceIcons[resource.type]
          const isSelected = selectedResource?.id === resource.id
          
          return (
            <CircleMarker
              key={resource.id}
              center={[resource.location.lat, resource.location.lng]}
              radius={isSelected ? 12 : 8}
              color={color}
              fillColor={color}
              fillOpacity={0.8}
              weight={isSelected ? 3 : 1}
              onClick={() => setSelectedResource(resource)}
            >
              <Popup>
                <div className="p-2 min-w-[200px]">
                  <div className="flex items-center gap-2 mb-2">
                    <Icon className="h-5 w-5" style={{ color }} />
                    <h3 className="font-semibold">{resource.name}</h3>
                  </div>
                  <p className="text-sm mb-2">{resource.description || 'No description'}</p>
                  <div className="flex items-center gap-2 text-xs text-text-secondary">
                    <span>Confidence: {Math.round(resource.confidence * 100)}%</span>
                    <span className={`px-1.5 py-0.5 rounded text-xs ${resource.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' : resource.severity === 'WARNING' ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}`}>
                      {resource.severity}
                    </span>
                  </div>
                </div>
              </Popup>
            </CircleMarker>
          )
        })}
      </MapContainer>
    </div>
  )
}