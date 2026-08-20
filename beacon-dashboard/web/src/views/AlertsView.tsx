import { useBeaconStore } from '../stores/beaconStore'
import { 
  AlertTriangle, AlertCircle, Bell, X, Plus, Filter, 
  MapPin, Clock, ChevronDown, Megaphone 
} from 'lucide-react'
import React from 'react'

const alertTypes = [
  { type: 'EVACUATION', label: 'Evacuation', icon: AlertTriangle, color: 'bg-red-500' },
  { type: 'BOIL_WATER', label: 'Boil Water', icon: AlertCircle, color: 'bg-blue-500' },
  { type: 'ROAD_CLOSURE', label: 'Road Closure', icon: AlertTriangle, color: 'bg-orange-500' },
  { type: 'WEATHER', label: 'Weather', icon: AlertCircle, color: 'bg-purple-500' },
  { type: 'SECURITY', label: 'Security', icon: AlertCircle, color: 'bg-gray-500' },
  { type: 'GENERAL', label: 'General', icon: Bell, color: 'bg-green-500' },
] as const

type AlertType = typeof alertTypes[number]['type']

export function AlertsView() {
  const { 
    alerts, 
    addAlert, 
    dismissAlert,
    node 
  } = useBeaconStore()
  
  const [filterType, setFilterType] = React.useState<AlertType | 'ALL'>('ALL')
  const [showForm, setShowForm] = React.useState(false)
  const [formData, setFormData] = React.useState({
    type: 'GENERAL' as AlertType,
    title: '',
    message: '',
    severity: 'INFO' as 'INFO' | 'WARNING' | 'CRITICAL',
    area: { north: '', south: '', east: '', west: '' },
    expiresIn: '3600', // seconds
  })

  const filteredAlerts = filterType === 'ALL' 
    ? alerts 
    : alerts.filter(a => a.type === filterType)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!node) return
    
    const alert = {
      id: crypto.randomUUID(),
      type: formData.type,
      title: formData.title,
      message: formData.message,
      severity: formData.severity,
      area: formData.area.north ? {
        north: parseFloat(formData.area.north),
        south: parseFloat(formData.area.south),
        east: parseFloat(formData.area.east),
        west: parseFloat(formData.area.west),
      } : undefined,
      expiresAt: Date.now() + parseInt(formData.expiresIn) * 1000,
      createdAt: Date.now(),
    }
    
    addAlert(alert)
    setFormData({ type: 'GENERAL', title: '', message: '', severity: 'INFO', area: { north: '', south: '', east: '', west: '' }, expiresIn: '3600' })
    setShowForm(false)
  }

  return (
    <div className="h-full p-5 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Alerts</h1>
          <p className="text-text-secondary">Emergency alerts and community notifications</p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors"
        >
          <Plus className="h-5 w-5" />
          Broadcast Alert
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        <button
          onClick={() => setFilterType('ALL')}
          className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
            filterType === 'ALL' ? 'bg-primary text-white' : 'bg-surface-variant text-text-secondary hover:bg-surface'
          }`}
        >
          All
        </button>
        {alertTypes.map(({ type, label, icon: Icon, color }) => (
          <button
            key={type}
            onClick={() => setFilterType(type)}
            className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
              filterType === type 
                ? `bg-${color.replace('bg-', '')}/10 text-${color.replace('bg-', '')}-700` 
                : 'bg-surface-variant text-text-secondary hover:bg-surface'
            }`}
          >
            <Icon className="h-4 w-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Alerts List */}
      <div className="bg-surface rounded-lg border border overflow-hidden">
        {filteredAlerts.length === 0 ? (
          <div className="p-12 text-center text-text-secondary">
            <Bell className="h-12 w-12 mx-auto mb-4 opacity-30" />
            <p className="text-lg">No alerts</p>
            <p className="text-text-secondary mt-1">
              {filterType !== 'ALL' ? `No ${filterType.toLowerCase()} alerts` : 'No active alerts'}
            </p>
          </div>
        ) : (
          <div className="divide-y divide">
            {filteredAlerts.map((alert) => {
              const typeInfo = alertTypes.find(t => t.type === alert.type)
              const Icon = typeInfo?.icon || Bell
              const isExpired = alert.expiresAt < Date.now()
              
              return (
                <div 
                  key={alert.id} 
                  className={`p-4 hover:bg-surface-variant transition-colors ${isExpired ? 'opacity-60' : ''}`}
                >
                  <div className="flex items-start gap-4">
                    <div className={`p-2 rounded-lg ${typeInfo?.color}/10`}>
                      <Icon className="h-5 w-5" style={{ color: typeInfo?.color.replace('bg-', '') }} />
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-semibold truncate">{alert.title}</h3>
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 text-xs rounded-full ${
                            alert.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                            alert.severity === 'WARNING' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-green-100 text-green-700'
                          }`}>
                            {alert.severity}
                          </span>
                          {isExpired && (
                            <span className="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-700">
                              Expired
                            </span>
                          )}
                        </div>
                      </div>
                      
                      <p className="text-sm text-text-secondary mt-1">{alert.message}</p>
                      
                      <div className="flex items-center gap-4 mt-2 text-xs text-text-secondary">
                        <span className="flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          Expires: {new Date(alert.expiresAt).toLocaleString()}
                        </span>
                        {alert.area && (
                          <span className="flex items-center gap-1">
                            <MapPin className="h-3 w-3" />
                            Geographic area
                          </span>
                        )}
                        <span>Created: {new Date(alert.createdAt).toLocaleString()}</span>
                      </div>
                    </div>
                    
                    <button
                      onClick={() => dismissAlert(alert.id)}
                      className="p-1.5 rounded hover:bg-red-100 text-red-500 transition-colors"
                      aria-label="Dismiss alert"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Broadcast Alert Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="bg-surface rounded-lg shadow-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-5 border-b border flex items-center justify-between">
              <h2 className="text-lg font-semibold">Broadcast Alert</h2>
              <button
                onClick={() => setShowForm(false)}
                className="p-1 rounded hover:bg-surface-variant transition-colors"
              >
                <ChevronDown className="h-5 w-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Alert Type</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value as AlertType })}
                    className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    {alertTypes.map(({ type, label }) => (
                      <option key={type} value={type}>{label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">Severity</label>
                  <select
                    value={formData.severity}
                    onChange={(e) => setFormData({ ...formData, severity: e.target.value as 'INFO' | 'WARNING' | 'CRITICAL' })}
                    className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    <option value="INFO">Info</option>
                    <option value="WARNING">Warning</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Title</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  placeholder="e.g., Evacuation Order Zone A"
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Message</label>
                <textarea
                  value={formData.message}
                  onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                  placeholder="Detailed description of the alert..."
                  rows={4}
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Geographic Area (Optional)</label>
                <div className="grid grid-cols-4 gap-3">
                  <input type="number" step="any" placeholder="North" value={formData.area.north} onChange={(e) => setFormData({ ...formData, area: { ...formData.area, north: e.target.value } })} className="px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
                  <input type="number" step="any" placeholder="South" value={formData.area.south} onChange={(e) => setFormData({ ...formData, area: { ...formData.area, south: e.target.value } })} className="px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
                  <input type="number" step="any" placeholder="East" value={formData.area.east} onChange={(e) => setFormData({ ...formData, area: { ...formData.area, east: e.target.value } })} className="px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
                  <input type="number" step="any" placeholder="West" value={formData.area.west} onChange={(e) => setFormData({ ...formData, area: { ...formData.area, west: e.target.value } })} className="px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
                </div>
                <p className="text-xs text-text-secondary mt-1">Bounding box coordinates (decimal degrees)</p>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Expires In</label>
                <select
                  value={formData.expiresIn}
                  onChange={(e) => setFormData({ ...formData, expiresIn: e.target.value })}
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="1800">30 minutes</option>
                  <option value="3600">1 hour</option>
                  <option value="7200">2 hours</option>
                  <option value="21600">6 hours</option>
                  <option value="43200">12 hours</option>
                  <option value="86400">24 hours</option>
                </select>
              </div>
              
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="flex-1 px-4 py-2 rounded-lg border border text-text-secondary hover:bg-surface-variant transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors"
                >
                  <Megaphone className="h-5 w-5 mr-2 inline" />
                  Broadcast Alert
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}