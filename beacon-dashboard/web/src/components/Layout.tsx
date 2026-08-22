import { Outlet, Link, useLocation, NavLink } from 'react-router-dom'
import { useBeaconStore } from '../stores/beaconStore'
import { 
  MapPin, MessageSquare, Network, Database, AlertTriangle, Settings, 
  Battery, Wifi, Bluetooth, Map, ChevronDown 
} from 'lucide-react'

const navItems = [
  { path: '/map', label: 'Map', icon: Map },
  { path: '/messages', label: 'Messages', icon: MessageSquare },
  { path: '/network', label: 'Network', icon: Network },
  { path: '/resources', label: 'Resources', icon: Database },
  { path: '/alerts', label: 'Alerts', icon: AlertTriangle },
  { path: '/settings', label: 'Settings', icon: Settings },
]

export function Layout() {
  const location = useLocation()
  const { node, connectionStats } = useBeaconStore()
  const [sidebarOpen, setSidebarOpen] = React.useState(true)

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <aside 
        className={`fixed inset-y-0 left-0 z-50 bg-surface border-r border transition-all duration-300 ${sidebarOpen ? 'w-64' : 'w-20'}`}
      >
        <div className="flex h-full flex-col">
          {/* Header */}
          <div className="flex h-16 items-center justify-between px-4 border-b border">
            <Link to="/map" className="flex items-center gap-2" aria-label="Beacon Dashboard">
              <MapPin className="h-6 w-6 text-primary" />
              <span className={`font-semibold text-lg ${sidebarOpen ? 'opacity-100' : 'opacity-0 invisible'}`}>
                BEACON
              </span>
            </Link>
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-2 rounded hover:bg-surface-variant transition-colors"
              aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
            >
              <ChevronDown className={`h-5 w-5 transition-transform ${sidebarOpen ? 'rotate-180' : ''}`} />
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto p-3 space-y-1" role="navigation" aria-label="Main navigation">
            {navItems.map(({ path, label, icon: Icon }) => {
              const isActive = location.pathname === path
              return (
                <NavLink
                  key={path}
                  to={path}
                  className={({ isActive }) => `
                    flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200
                    ${isActive 
                      ? 'bg-primary/10 text-primary' 
                      : 'text-text-secondary hover:bg-surface-variant hover:text-text'
                    }
                    ${sidebarOpen ? '' : 'justify-center'}
                  `}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon className="h-5 w-5 flex-shrink-0" aria-hidden="true" />
                  {sidebarOpen && <span className="font-medium">{label}</span>}
                </NavLink>
              )
            })}
          </nav>

          {/* Node Status */}
          <div className="p-3 border-t border" aria-label="Node status">
            <div className="flex items-center gap-3">
              <div className={`w-3 h-3 rounded-full ${node?.isOnline ? 'bg-success' : 'bg-error'}`} />
              <div className={`flex-1 min-w-0 ${sidebarOpen ? '' : 'hidden'}`}>
                <p className="text-sm font-medium truncate">{node?.displayName || 'Beacon Node'}</p>
                <p className="text-xs text-text-secondary truncate">
                  {node?.peerId ? node.peerId.substring(0, 12) + '...' : 'Unknown'}
                </p>
              </div>
            </div>
            {sidebarOpen && (
              <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                <div className="p-2 rounded bg-surface-variant">
                  <Battery className="h-4 w-4 mx-auto text-text-secondary" />
                  <p className="text-xs font-medium mt-1">{node?.batteryLevel || 0}%</p>
                </div>
                <div className="p-2 rounded bg-surface-variant">
                  <Bluetooth className="h-4 w-4 mx-auto text-text-secondary" />
                  <p className="text-xs font-medium mt-1">{connectionStats?.ble || 0}</p>
                </div>
                <div className="p-2 rounded bg-surface-variant">
                  <Wifi className="h-4 w-4 mx-auto text-text-secondary" />
                  <p className="text-xs font-medium mt-1">{connectionStats?.wifi || 0}</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main 
        className={`flex-1 overflow-auto transition-all duration-300 ${sidebarOpen ? 'ml-64' : 'ml-20'}`}
        role="main"
      >
        <Outlet />
      </main>
    </div>
  )
}