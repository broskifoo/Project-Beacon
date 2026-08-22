import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface Peer {
  id: string
  displayName?: string
  lastSeen: number
  location?: { lat: number; lng: number }
  batteryLevel?: number
  powerMode?: string
  transports: string[]
  signalStrength?: number
  isOnline: boolean
  isTrusted: boolean
}

export interface NodeInfo {
  id: string
  displayName?: string
  peerId?: string
  batteryLevel: number
  isOnline: boolean
}

export interface ConnectionStats {
  ble: number
  wifi: number
  lora: number
  total: number
}

export interface Message {
  id: string
  senderId: string
  recipientId?: string
  timestamp: number
  priority: 'CRITICAL' | 'HIGH' | 'NORMAL' | 'LOW'
  type: string
  text?: string
  status: 'QUEUED' | 'SENDING' | 'SENT' | 'DELIVERED' | 'ACKNOWLEDGED' | 'FAILED' | 'EXPIRED'
}

export interface Resource {
  id: string
  type: 'WATER' | 'FOOD' | 'MEDICAL' | 'SHELTER' | 'CHARGING' | 'HAZARD' | 'ROAD_CLOSED'
  name: string
  location: { lat: number; lng: number }
  description?: string
  confidence: number
  severity: 'INFO' | 'WARNING' | 'CRITICAL'
  createdAt: number
  expiresAt?: number
}

export interface Alert {
  id: string
  type: 'EVACUATION' | 'BOIL_WATER' | 'ROAD_CLOSURE' | 'WEATHER' | 'SECURITY' | 'GENERAL'
  title: string
  message: string
  severity: 'INFO' | 'WARNING' | 'CRITICAL'
  area?: { north: number; south: number; east: number; west: number }
  expiresAt: number
  createdAt: number
}

interface BeaconState {
  // Node info
  node: NodeInfo | null
  connectionStats: ConnectionStats
  
  // Network
  peers: Peer[]
  topology: { nodes: any[]; links: any[] } | null
  
  // Messages
  messages: Message[]
  selectedPeerId: string | null
  
  // Resources
  resources: Resource[]
  
  // Alerts
  alerts: Alert[]
  
  // UI state
  mapCenter: { lat: number; lng: number }
  mapZoom: number
  selectedResource: Resource | null
  
  // Actions
  initialize: () => Promise<void>
  setNode: (node: NodeInfo) => void
  updateConnectionStats: (stats: Partial<ConnectionStats>) => void
  addPeer: (peer: Peer) => void
  updatePeer: (id: string, updates: Partial<Peer>) => void
  removePeer: (id: string) => void
  setTopology: (topology: any) => void
  addMessage: (message: Message) => void
  updateMessage: (id: string, updates: Partial<Message>) => void
  setSelectedPeer: (id: string | null) => void
  addResource: (resource: Resource) => void
  updateResource: (id: string, updates: Partial<Resource>) => void
  removeResource: (id: string) => void
  addAlert: (alert: Alert) => void
  dismissAlert: (id: string) => void
  setMapView: (center: { lat: number; lng: number }, zoom: number) => void
  setSelectedResource: (resource: Resource | null) => void
}

export const useBeaconStore = create<BeaconState>()(
  persist(
    (set, get) => ({
      // Initial state
      node: null,
      connectionStats: { ble: 0, wifi: 0, lora: 0, total: 0 },
      peers: [],
      topology: null,
      messages: [],
      selectedPeerId: null,
      resources: [],
      alerts: [],
      mapCenter: { lat: 0, lng: 0 },
      mapZoom: 12,
      selectedResource: null,

      // Actions
      initialize: async () => {
        // Initialize Beacon SDK connection
        // This will be implemented when SDK is ready
        console.log('Initializing Beacon Dashboard...')
      },

      setNode: (node) => set({ node }),
      
      updateConnectionStats: (stats) => set((state) => ({
        connectionStats: { ...state.connectionStats, ...stats }
      })),

      addPeer: (peer) => set((state) => ({
        peers: [...state.peers.filter(p => p.id !== peer.id), peer]
      })),

      updatePeer: (id, updates) => set((state) => ({
        peers: state.peers.map(p => p.id === id ? { ...p, ...updates } : p)
      })),

      removePeer: (id) => set((state) => ({
        peers: state.peers.filter(p => p.id !== id)
      })),

      setTopology: (topology) => set({ topology }),

      addMessage: (message) => set((state) => ({
        messages: [message, ...state.messages].slice(0, 1000)
      })),

      updateMessage: (id, updates) => set((state) => ({
        messages: state.messages.map(m => m.id === id ? { ...m, ...updates } : m)
      })),

      setSelectedPeer: (id) => set({ selectedPeerId: id }),

      addResource: (resource) => set((state) => ({
        resources: [...state.resources.filter(r => r.id !== resource.id), resource]
      })),

      updateResource: (id, updates) => set((state) => ({
        resources: state.resources.map(r => r.id === id ? { ...r, ...updates } : r)
      })),

      removeResource: (id) => set((state) => ({
        resources: state.resources.filter(r => r.id !== id)
      })),

      addAlert: (alert) => set((state) => ({
        alerts: [alert, ...state.alerts].slice(0, 100)
      })),

      dismissAlert: (id) => set((state) => ({
        alerts: state.alerts.filter(a => a.id !== id)
      })),

      setMapView: (center, zoom) => set({ mapCenter: center, mapZoom: zoom }),

      setSelectedResource: (resource) => set({ selectedResource: resource }),
    }),
    {
      name: 'beacon-dashboard-storage',
      partialize: (state) => ({
        mapCenter: state.mapCenter,
        mapZoom: state.mapZoom,
        node: state.node,
      }),
    }
  )
)