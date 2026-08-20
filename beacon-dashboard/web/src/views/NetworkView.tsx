import { useBeaconStore } from '../stores/beaconStore'
import { Network as NetworkIcon, Battery, Wifi, Bluetooth, Signal, TrendingUp, TrendingDown, Minus } from 'lucide-react'

export function NetworkView() {
  const { peers, topology, connectionStats } = useBeaconStore()

  return (
    <div className="h-full p-5 space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Peers"
          value={peers.filter(p => p.isOnline).length}
          icon={Users}
          trend={peers.filter(p => p.isOnline).length > 5 ? 'up' : 'neutral'}
        />
        <StatCard
          title="BLE Connections"
          value={connectionStats.ble}
          icon={Bluetooth}
          color="text-blue-500"
        />
        <StatCard
          title="Wi-Fi Direct"
          value={connectionStats.wifi}
          icon={Wifi}
          color="text-green-500"
        />
        <StatCard
          title="LoRa Nodes"
          value={connectionStats.lora}
          icon={Signal}
          color="text-purple-500"
        />
      </div>

      {/* Peer List */}
      <div className="bg-surface rounded-lg border border overflow-hidden">
        <div className="px-5 py-4 border-b border flex items-center justify-between">
          <h2 className="text-lg font-semibold">Nearby Peers ({peers.filter(p => p.isOnline).length})</h2>
          <span className="px-2 py-1 text-xs rounded-full bg-primary/10 text-primary">
            Live
          </span>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-xs font-medium text-text-secondary uppercase tracking-wider bg-surface-variant">
                <th className="px-5 py-3">Peer</th>
                <th className="px-5 py-3">Transports</th>
                <th className="px-5 py-3">Signal</th>
                <th className="px-5 py-3">Battery</th>
                <th className="px-5 py-3">Power Mode</th>
                <th className="px-5 py-3">Last Seen</th>
                <th className="px-5 py-3">Trust</th>
              </tr>
            </thead>
            <tbody className="divide-y divide">
              {peers
                .filter(p => p.isOnline)
                .sort((a, b) => (b.signalStrength || -100) - (a.signalStrength || -100))
                .map((peer) => (
                  <tr key={peer.id} className="hover:bg-surface-variant transition-colors">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                          <span className="text-xs font-medium text-primary">
                            {peer.displayName ? peer.displayName[0].toUpperCase() : peer.id[0].toUpperCase()}
                          </span>
                        </div>
                        <div>
                          <p className="font-medium truncate max-w-xs">
                            {peer.displayName || `Peer ${peer.id.substring(0, 8)}`}
                          </p>
                          <p className="text-xs text-text-secondary truncate max-w-xs">
                            {peer.id.substring(0, 12)}...
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex gap-1">
                        {peer.transports.includes('ble') && <Bluetooth className="h-4 w-4 text-blue-500" title="BLE" />}
                        {peer.transports.includes('wifi_direct') && <Wifi className="h-4 w-4 text-green-500" title="Wi-Fi Direct" />}
                        {peer.transports.includes('lora') && <Signal className="h-4 w-4 text-purple-500" title="LoRa" />}
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <Signal className={`h-3 w-3 ${peer.signalStrength !== undefined && peer.signalStrength > -70 ? 'text-green-500' : peer.signalStrength !== undefined && peer.signalStrength > -85 ? 'text-yellow-500' : 'text-red-500'}`} />
                        <span className="text-sm font-mono">
                          {peer.signalStrength !== undefined ? `${peer.signalStrength} dBm` : 'N/A'}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <Battery className={`h-3 w-3 ${peer.batteryLevel !== undefined && peer.batteryLevel > 50 ? 'text-green-500' : peer.batteryLevel !== undefined && peer.batteryLevel > 20 ? 'text-yellow-500' : 'text-red-500'}`} />
                        <span className="text-sm">
                          {peer.batteryLevel !== undefined ? `${peer.batteryLevel}%` : 'N/A'}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <span className={`px-2 py-0.5 text-xs rounded-full ${
                        peer.powerMode === 'NORMAL' ? 'bg-gray-100 text-gray-700' :
                        peer.powerMode === 'CONSERVATION' ? 'bg-yellow-100 text-yellow-700' :
                        peer.powerMode === 'SURVIVAL' ? 'bg-orange-100 text-orange-700' :
                        'bg-red-100 text-red-700'
                      }`}>
                        {peer.powerMode || 'UNKNOWN'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-sm text-text-secondary">
                      {peer.lastSeen ? formatRelativeTime(peer.lastSeen) : 'Never'}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <div className="w-16 h-1.5 bg-surface-variant rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-primary rounded-full transition-all"
                            style={{ width: `${(peer.isTrusted ? 100 : peer.trustScore * 100)}%` }}
                          />
                        </div>
                        <span className="text-xs text-text-secondary">
                          {Math.round((peer.isTrusted ? 1 : peer.trustScore) * 100)}%
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              {peers.filter(p => p.isOnline).length === 0 && (
                <tr>
                  <td colSpan={7} className="px-5 py-8 text-center text-text-secondary">
                    No peers discovered yet. Ensure Bluetooth and Location are enabled.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Topology Visualization Placeholder */}
      {topology && (
        <div className="bg-surface rounded-lg border border p-5">
          <h3 className="text-lg font-semibold mb-4">Mesh Topology</h3>
          <div className="h-64 bg-surface-variant rounded flex items-center justify-center text-text-secondary">
            Topology visualization (Cytoscape.js integration planned)
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ 
  title, 
  value, 
  icon: Icon, 
  color = 'text-primary', 
  trend = 'neutral' 
}: {
  title: string
  value: number | string
  icon: React.ComponentType<{ className?: string }>
  color?: string
  trend?: 'up' | 'down' | 'neutral'
}) {
  const TrendIcon = trend === 'up' ? TrendingUp : trend === 'down' ? TrendingDown : Minus
  const trendColor = trend === 'up' ? 'text-green-500' : trend === 'down' ? 'text-red-500' : 'text-text-secondary'
  
  return (
    <div className="bg-surface rounded-lg border border p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-text-secondary">{title}</p>
          <p className="text-3xl font-bold mt-1">{value}</p>
        </div>
        <div className={`p-3 rounded-lg bg-primary/10 ${color}`}>
          <Icon className="h-6 w-6" />
        </div>
      </div>
      <div className="flex items-center gap-1 mt-3 text-xs">
        <TrendIcon className={trendColor} />
        <span className={trendColor}>
          {trend === 'up' ? '+12%' : trend === 'down' ? '-5%' : 'Stable'}
        </span>
        <span className="text-text-secondary">vs 1h ago</span>
      </div>
    </div>
  )
}

function formatRelativeTime(timestamp: number): string {
  const diff = Date.now() - timestamp
  if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`
  return new Date(timestamp).toLocaleDateString()
}