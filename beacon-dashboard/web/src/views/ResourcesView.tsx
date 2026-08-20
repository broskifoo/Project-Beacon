import { useBeaconStore } from '../stores/beaconStore'
import { 
  Droplet, Utensils, Heart, Home, Battery, AlertTriangle, 
  Plus, Filter, MapPin, Check, AlertCircle, ChevronDown 
} from 'lucide-react'
import React from 'react'

const resourceTypes = [
  { type: 'WATER', label: 'Water', icon: Droplet, color: 'bg-blue-500' },
  { type: 'FOOD', label: 'Food', icon: Utensils, color: 'bg-green-500' },
  { type: 'MEDICAL', label: 'Medical', icon: Heart, color: 'bg-red-500' },
  { type: 'SHELTER', label: 'Shelter', icon: Home, color: 'bg-purple-500' },
  { type: 'CHARGING', label: 'Charging', icon: Battery, color: 'bg-yellow-500' },
  { type: 'HAZARD', label: 'Hazards', icon: AlertTriangle, color: 'bg-red-500' },
  { type: 'ROAD_CLOSED', label: 'Road Closed', icon: AlertTriangle, color: 'bg-orange-500' },
] as const

type ResourceType = typeof resourceTypes[number]['type']

export function ResourcesView() {
  const { 
    resources, 
    addResource, 
    updateResource, 
    removeResource,
    setSelectedResource,
    selectedResource 
  } = useBeaconStore()
  
  const [filterType, setFilterType] = React.useState<ResourceType | 'ALL'>('ALL')
  const [showForm, setShowForm] = React.useState(false)
  const [formData, setFormData] = React.useState({
    type: 'WATER' as ResourceType,
    name: '',
    lat: '',
    lng: '',
    description: '',
    severity: 'INFO' as 'INFO' | 'WARNING' | 'CRITICAL',
    confidence: 0.5,
  })

  const filteredResources = filterType === 'ALL' 
    ? resources 
    : resources.filter(r => r.type === filterType)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    const resource = {
      id: crypto.randomUUID(),
      type: formData.type,
      name: formData.name,
      location: { lat: parseFloat(formData.lat), lng: parseFloat(formData.lng) },
      description: formData.description,
      severity: formData.severity,
      confidence: formData.confidence,
      createdAt: Date.now(),
      expiresAt: Date.now() + 24 * 60 * 60 * 1000, // 24 hours
    }
    
    addResource(resource)
    setFormData({ type: 'WATER', name: '', lat: '', lng: '', description: '', severity: 'INFO', confidence: 0.5 })
    setShowForm(false)
  }

  const ResourceTypeIcon = resourceTypes.find(r => r.type === formData.type)?.icon || Droplet

  return (
    <div className="h-full p-5 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Resources</h1>
          <p className="text-text-secondary">Community-reported resources and hazards</p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white hover:bg-primary/90 transition-colors"
        >
          <Plus className="h-5 w-5" />
          Report Resource
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        <button
          onClick={() => setFilterType('ALL')}
          className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
            filterType === 'ALL' 
              ? 'bg-primary text-white' 
              : 'bg-surface-variant text-text-secondary hover:bg-surface'
          }`}
        >
          All
        </button>
        {resourceTypes.map(({ type, label, icon: Icon, color }) => (
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

      {/* Resource List */}
      <div className="bg-surface rounded-lg border border overflow-hidden">
        {filteredResources.length === 0 ? (
          <div className="p-12 text-center text-text-secondary">
            <MapPin className="h-12 w-12 mx-auto mb-4 opacity-30" />
            <p className="text-lg">No resources found</p>
            <p className="text-text-secondary mt-1">
              {filterType !== 'ALL' ? `No ${filterType.toLowerCase()} resources reported` : 'Be the first to report a resource'}
            </p>
            <button
              onClick={() => setShowForm(true)}
              className="mt-4 px-4 py-2 rounded-lg bg-primary text-white hover:bg-primary/90"
            >
              Report First Resource
            </button>
          </div>
        ) : (
          <div className="divide-y divide">
            {filteredResources.map((resource) => {
              const typeInfo = resourceTypes.find(r => r.type === resource.type)
              const Icon = typeInfo?.icon || Droplet
              const isSelected = selectedResource?.id === resource.id
              
              return (
                <div
                  key={resource.id}
                  className={`p-4 hover:bg-surface-variant transition-colors cursor-pointer ${isSelected ? 'bg-primary/5' : ''}`}
                  onClick={() => setSelectedResource(resource)}
                >
                  <div className="flex items-start gap-4">
                    <div className={`p-2 rounded-lg ${typeInfo?.color}/10`}>
                      <Icon className="h-5 w-5" style={{ color: typeInfo?.color.replace('bg-', '') }} />
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-semibold truncate">{resource.name}</h3>
                        <span className={`px-2 py-0.5 text-xs rounded-full ${
                          resource.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                          resource.severity === 'WARNING' ? 'bg-yellow-100 text-yellow-700' :
                          'bg-green-100 text-green-700'
                        }`}>
                          {resource.severity}
                        </span>
                      </div>
                      
                      <p className="text-sm text-text-secondary mt-1 truncate">{resource.description}</p>
                      
                      <div className="flex items-center gap-4 mt-2 text-xs text-text-secondary">
                        <span className="flex items-center gap-1">
                          <MapPin className="h-3 w-3" />
                          {resource.location.lat.toFixed(4)}, {resource.location.lng.toFixed(4)}
                        </span>
                        <span>Confidence: {Math.round(resource.confidence * 100)}%</span>
                        <span>Expires: {new Date(resource.expiresAt || 0).toLocaleString()}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-2">
                      <span className={`w-2 h-2 rounded-full ${resource.severity === 'CRITICAL' ? 'bg-red-500' : resource.severity === 'WARNING' ? 'bg-yellow-500' : 'bg-green-500'}`} />
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          removeResource(resource.id)
                        }}
                        className="p-1.5 rounded hover:bg-red-100 text-red-500 transition-colors"
                        aria-label="Delete resource"
                      >
                        <AlertCircle className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Add Resource Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="bg-surface rounded-lg shadow-lg max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div className="p-5 border-b border flex items-center justify-between">
              <h2 className="text-lg font-semibold">Report Resource</h2>
              <button
                onClick={() => setShowForm(false)}
                className="p-1 rounded hover:bg-surface-variant transition-colors"
              >
                <ChevronDown className="h-5 w-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Type</label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value as ResourceType })}
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  {resourceTypes.map(({ type, label }) => (
                    <option key={type} value={type}>{label}</option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., Community Center Well"
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                  required
                />
              </div>
              
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium mb-1">Latitude</label>
                  <input
                    type="number"
                    step="any"
                    value={formData.lat}
                    onChange={(e) => setFormData({ ...formData, lat: e.target.value })}
                    placeholder="40.7128"
                    className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Longitude</label>
                  <input
                    type="number"
                    step="any"
                    value={formData.lng}
                    onChange={(e) => setFormData({ ...formData, lng: e.target.value })}
                    placeholder="-74.0060"
                    className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                    required
                  />
                </div>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="e.g., Working hand pump, good taste"
                  rows={3}
                  className="w-full px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
                />
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
              
              <div>
                <label className="block text-sm font-medium mb-1">Confidence: {Math.round(formData.confidence * 100)}%</label>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={formData.confidence}
                  onChange={(e) => setFormData({ ...formData, confidence: parseFloat(e.target.value) })}
                  className="w-full"
                />
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
                  className="flex-1 px-4 py-2 rounded-lg bg-primary text-white hover:bg-primary/90 transition-colors"
                >
                  Submit Report
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}