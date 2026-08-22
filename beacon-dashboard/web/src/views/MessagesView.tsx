import { useBeaconStore } from '../stores/beaconStore'
import { Send, ChevronDown, AlertCircle, ArrowUpRight, CheckCheck, Clock, X } from 'lucide-react'
import React from 'react'

const priorityStyles = {
  CRITICAL: 'bg-red-100 text-red-700 border-red-200',
  HIGH: 'bg-orange-100 text-orange-700 border-orange-200',
  NORMAL: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  LOW: 'bg-green-100 text-green-700 border-green-200',
}

const statusIcons = {
  QUEUED: Clock,
  SENDING: ArrowUpRight,
  SENT: Send,
  DELIVERED: CheckCheck,
  ACKNOWLEDGED: CheckCheck,
  FAILED: X,
  EXPIRED: AlertCircle,
}

export function MessagesView() {
  const { 
    messages, 
    peers, 
    selectedPeerId, 
    setSelectedPeer,
    addMessage,
    node 
  } = useBeaconStore()
  
  const [newMessage, setNewMessage] = React.useState('')
  const [showPeerSelect, setShowPeerSelect] = React.useState(false)

  const filteredMessages = selectedPeerId 
    ? messages.filter(m => m.senderId === selectedPeerId || m.recipientId === selectedPeerId)
    : messages

  const handleSend = () => {
    if (!newMessage.trim() || !selectedPeerId) return
    
    addMessage({
      id: crypto.randomUUID(),
      senderId: node?.id || 'local',
      recipientId: selectedPeerId,
      timestamp: Date.now(),
      priority: 'NORMAL',
      type: 'TEXT',
      text: newMessage,
      status: 'SENT',
    })
    
    setNewMessage('')
  }

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex h-16 items-center justify-between px-5 border-b border">
        <div className="flex items-center gap-3">
          <select
            value={selectedPeerId || ''}
            onChange={(e) => setSelectedPeer(e.target.value || null)}
            className="px-3 py-2 rounded-lg border border bg-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            aria-label="Select peer"
          >
            <option value="">All Peers</option>
            {peers.filter(p => p.isOnline).map((peer) => (
              <option key={peer.id} value={peer.id}>
                {peer.displayName || `Peer ${peer.id.substring(0, 8)}`}
              </option>
            ))}
          </select>
          {selectedPeerId && (
            <button
              onClick={() => setSelectedPeer(null)}
              className="text-sm text-primary hover:underline"
            >
              Clear filter
            </button>
          )}
        </div>
      </div>

      {/* Messages List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {filteredMessages.length === 0 ? (
          <div className="flex h-full items-center justify-center text-text-secondary">
            <p>{selectedPeerId ? 'No messages with this peer' : 'No messages yet'}</p>
          </div>
        ) : (
          filteredMessages.map((msg) => {
            const isOwn = msg.senderId === node?.id
            const StatusIcon = statusIcons[msg.status]
            const priorityClass = priorityStyles[msg.priority]
            
            return (
              <div
                key={msg.id}
                className={`flex gap-3 ${isOwn ? 'flex-row-reverse' : ''}`}
              >
                {!isOwn && (
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-primary">
                      {msg.senderId.substring(0, 2).toUpperCase()}
                    </span>
                  </div>
                )}
                
                <div className={`flex-1 ${isOwn ? 'text-right' : ''}`}>
                  <div className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium border ${priorityClass}`}>
                    <span>{msg.priority}</span>
                    <StatusIcon className="h-3 w-3" />
                  </div>
                  
                  <div className={`mt-1 max-w-xs ${isOwn ? 'ml-auto' : ''}`}>
                    <div className={`bg-surface p-3 rounded-2xl ${isOwn ? 'bg-primary text-white rounded-tr-none' : 'border border rounded-tl-none'}`}>
                      {msg.text}
                    </div>
                    <div className="flex items-center gap-2 mt-1 text-xs text-text-secondary">
                      <span>{new Date(msg.timestamp).toLocaleTimeString()}</span>
                      <StatusIcon className="h-3 w-3" />
                      <span className="capitalize">{msg.status.toLowerCase()}</span>
                    </div>
                  </div>
                </div>
                
                {isOwn && (
                  <div className="w-8 h-8 rounded-full bg-surface-variant flex items-center justify-center flex-shrink-0" />
                )}
              </div>
            )
          )}
        )}
      </div>

      {/* Composer */}
      <div className="p-4 border-t border">
        <div className="flex gap-2">
          <select
            value={selectedPeerId || ''}
            onChange={(e) => setSelectedPeer(e.target.value || null)}
            className="px-3 py-2 rounded-lg border border bg-surface text-sm w-40 focus:outline-none focus:ring-2 focus:ring-primary"
            disabled={peers.filter(p => p.isOnline).length === 0}
          >
            <option value="">Select peer</option>
            {peers.filter(p => p.isOnline).map((peer) => (
              <option key={peer.id} value={peer.id}>
                {peer.displayName || `Peer ${peer.id.substring(0, 8)}`}
              </option>
            ))}
          </select>
          
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Type a message..."
            className="flex-1 px-4 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
            disabled={!selectedPeerId}
          />
          
          <button
            onClick={handleSend}
            disabled={!newMessage.trim() || !selectedPeerId}
            className="px-4 py-2 rounded-lg bg-primary text-white font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Send className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  )
}