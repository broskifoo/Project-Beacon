import { useBeaconStore } from '../stores/beaconStore'
import { 
  User, Shield, Battery, Globe, Bell, HardDrive, 
  Download, Upload, Trash2, Key, Wifi, Bluetooth, 
  Radio, ToggleLeft, ToggleRight, ChevronDown 
} from 'lucide-react'
import React from 'react'

export function SettingsView() {
  const { node } = useBeaconStore()
  
  const [settings, setSettings] = React.useState({
    // Network
    enableBle: true,
    enableWifiDirect: true,
    enableLora: false,
    maxPeers: 50,
    defaultTtl: 5,
    
    // Power
    defaultPowerMode: 'NORMAL',
    autoSwitchConservation: true,
    autoSwitchSurvival: true,
    autoSwitchCritical: true,
    scanAggressiveness: 'balanced',
    
    // Security
    encryptStorage: true,
    autoRotateKeys: true,
    keyRotationDays: 90,
    
    // Maps
    autoUpdateMaps: true,
    updateOnWifiOnly: true,
    renderingMode: 'vector',
    
    // Notifications
    notifySos: true,
    notifyHighPriority: true,
    notifyNormal: false,
    vibrationEnabled: true,
    soundProfile: 'critical_only',
  })

  return (
    <div className="h-full p-5 space-y-6 overflow-y-auto">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-text-secondary">Configure Beacon Dashboard preferences</p>
      </div>

      {/* Network Settings */}
      <SettingsSection title="Network" icon={Globe}>
        <SettingItem
          label="Bluetooth Low Energy"
          description="Primary discovery and messaging transport"
          icon={Bluetooth}
        >
          <Toggle checked={settings.enableBle} onChange={(v) => setSettings({...settings, enableBle: v})} />
        </SettingItem>
        
        <SettingItem
          label="Wi-Fi Direct"
          description="High-bandwidth transfers (maps, images)"
          icon={Wifi}
        >
          <Toggle checked={settings.enableWifiDirect} onChange={(v) => setSettings({...settings, enableWifiDirect: v})} />
        </SettingItem>
        
        <SettingItem
          label="LoRa (External Radio)"
          description="Long-range communication via Beacon Radio hardware"
          icon={Radio}
        >
          <Toggle checked={settings.enableLora} onChange={(v) => setSettings({...settings, enableLora: v})} />
        </SettingItem>
        
        <SettingItem
          label="Max Peers"
          description="Maximum number of peers to track"
          icon={User}
        >
          <input
            type="number"
            value={settings.maxPeers}
            onChange={(e) => setSettings({...settings, maxPeers: parseInt(e.target.value)})}
            min="10"
            max="200"
            className="w-24 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </SettingItem>
        
        <SettingItem
          label="Default TTL (Hops)"
          description="Time-to-live for messages in mesh hops"
          icon={Radio}
        >
          <select
            value={settings.defaultTtl}
            onChange={(e) => setSettings({...settings, defaultTtl: parseInt(e.target.value)})}
            className="w-24 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {[1,2,3,4,5,7,10].map(n => <option key={n} value={n}>{n} hops</option>)}
          </select>
        </SettingItem>
      </SettingsSection>

      {/* Power Management */}
      <SettingsSection title="Power Management" icon={Battery}>
        <SettingItem
          label="Default Power Mode"
          description="Initial power mode on startup"
          icon={Battery}
        >
          <select
            value={settings.defaultPowerMode}
            onChange={(e) => setSettings({...settings, defaultPowerMode: e.target.value})}
            className="w-40 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="NORMAL">Normal</option>
            <option value="CONSERVATION">Conservation</option>
            <option value="SURVIVAL">Survival</option>
          </select>
        </SettingItem>
        
        <SettingItem
          label="Auto-switch to Conservation"
          description="Below 50% battery"
          icon={Battery}
        >
          <Toggle checked={settings.autoSwitchConservation} onChange={(v) => setSettings({...settings, autoSwitchConservation: v})} />
        </SettingItem>
        
        <SettingItem
          label="Auto-switch to Survival"
          description="Below 20% battery"
          icon={Battery}
        >
          <Toggle checked={settings.autoSwitchSurvival} onChange={(v) => setSettings({...settings, autoSwitchSurvival: v})} />
        </SettingItem>
        
        <SettingItem
          label="Auto-switch to Critical"
          description="Below 10% battery"
          icon={Battery}
        >
          <Toggle checked={settings.autoSwitchCritical} onChange={(v) => setSettings({...settings, autoSwitchCritical: v})} />
        </SettingItem>
        
        <SettingItem
          label="Background Scan Aggressiveness"
          description="How aggressively to scan for peers"
          icon={Radio}
        >
          <select
            value={settings.scanAggressiveness}
            onChange={(e) => setSettings({...settings, scanAggressiveness: e.target.value})}
            className="w-40 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="conservative">Conservative (max battery)</option>
            <option value="balanced">Balanced</option>
            <option value="aggressive">Aggressive (max discovery)</option>
          </select>
        </SettingItem>
      </SettingsSection>

      {/* Security */}
      <SettingsSection title="Security" icon={Shield}>
        <SettingItem
          label="Encrypt Local Storage"
          description="AES-256 encryption for all local data"
          icon={Shield}
        >
          <Toggle checked={settings.encryptStorage} onChange={(v) => setSettings({...settings, encryptStorage: v})} />
        </SettingItem>
        
        <SettingItem
          label="Auto-rotate Identity Keys"
          description="Generate new keys periodically"
          icon={Key}
        >
          <Toggle checked={settings.autoRotateKeys} onChange={(v) => setSettings({...settings, autoRotateKeys: v})} />
        </SettingItem>
        
        <SettingItem
          label="Key Rotation Interval"
          description="Days between automatic key rotations"
          icon={Key}
        >
          <input
            type="number"
            value={settings.keyRotationDays}
            onChange={(e) => setSettings({...settings, keyRotationDays: parseInt(e.target.value)})}
            min="30"
            max="365"
            className="w-24 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </SettingItem>
        
        <SettingItem
          label="View Identity Fingerprint"
          description="Your public key fingerprint for verification"
          icon={Key}
        >
          <div className="flex items-center gap-3">
            <code className="px-3 py-1.5 rounded bg-surface-variant font-mono text-sm">
              {node?.peerId ? node.peerId.substring(0, 32) + '...' : 'Not initialized'}
            </code>
            <button className="px-3 py-1.5 rounded border border text-sm text-text-secondary hover:bg-surface-variant">
              Copy
            </button>
          </div>
        </SettingItem>
        
        <SettingItem
          label="Trusted Peers"
          description="Manage peer trust relationships"
          icon={User}
        >
          <button className="px-3 py-1.5 rounded border border text-sm text-primary hover:bg-primary/5">
            Manage ({0} trusted)
          </button>
        </SettingItem>
      </SettingsSection>

      {/* Maps */}
      <SettingsSection title="Maps" icon={Globe}>
        <SettingItem
          label="Auto-update Maps"
          description="Download map updates automatically"
          icon={Download}
        >
          <Toggle checked={settings.autoUpdateMaps} onChange={(v) => setSettings({...settings, autoUpdateMaps: v})} />
        </SettingItem>
        
        <SettingItem
          label="Updates on Wi-Fi Only"
          description="Prevent cellular data usage for map downloads"
          icon={Wifi}
        >
          <Toggle checked={settings.updateOnWifiOnly} onChange={(v) => setSettings({...settings, updateOnWifiOnly: v})} />
        </SettingItem>
        
        <SettingItem
          label="Rendering Mode"
          description="Map rendering engine"
          icon={Globe}
        >
          <select
            value={settings.renderingMode}
            onChange={(e) => setSettings({...settings, renderingMode: e.target.value})}
            className="w-40 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="vector">Vector Tiles (Recommended)</option>
            <option value="raster">Raster Tiles</option>
          </select>
        </SettingItem>
        
        <SettingItem
          label="Manage Downloaded Maps"
          description="View and remove downloaded map regions"
          icon={HardDrive}
        >
          <button className="px-3 py-1.5 rounded border border text-sm text-primary hover:bg-primary/5">
            Manage (2.3 GB)
          </button>
        </SettingItem>
      </SettingsSection>

      {/* Notifications */}
      <SettingsSection title="Notifications" icon={Bell}>
        <SettingItem
          label="SOS Alerts"
          description="Always notify for emergency SOS"
          icon={Bell}
        >
          <Toggle checked={settings.notifySos} onChange={(v) => setSettings({...settings, notifySos: v})} />
        </SettingItem>
        
        <SettingItem
          label="High Priority Messages"
          description="Notify for urgent messages"
          icon={Bell}
        >
          <Toggle checked={settings.notifyHighPriority} onChange={(v) => setSettings({...settings, notifyHighPriority: v})} />
        </SettingItem>
        
        <SettingItem
          label="Normal Priority Messages"
          description="Notify for regular messages"
          icon={Bell}
        >
          <Toggle checked={settings.notifyNormal} onChange={(v) => setSettings({...settings, notifyNormal: v})} />
        </SettingItem>
        
        <SettingItem
          label="Vibration"
          description="Vibrate on notifications"
          icon={Bell}
        >
          <Toggle checked={settings.vibrationEnabled} onChange={(v) => setSettings({...settings, vibrationEnabled: v})} />
        </SettingItem>
        
        <SettingItem
          label="Sound Profile"
          description="When to play notification sounds"
          icon={Bell}
        >
          <select
            value={settings.soundProfile}
            onChange={(e) => setSettings({...settings, soundProfile: e.target.value})}
            className="w-48 px-3 py-2 rounded-lg border border bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="all">All Notifications</option>
            <option value="critical_only">Critical Only</option>
            <option value="none">Silent</option>
          </select>
        </SettingItem>
      </SettingsSection>

      {/* About */}
      <SettingsSection title="About" icon={Info}>
        <SettingItem
          label="Version"
          description="Current application version"
          icon={Info}
        >
          <span className="text-text-secondary font-mono">0.1.0-alpha</span>
        </SettingItem>
        
        <SettingItem
          label="Build Date"
          description="When this version was built"
          icon={Info}
        >
          <span className="text-text-secondary font-mono">2026-08-20</span>
        </SettingItem>
        
        <SettingItem
          label="License"
          description="MIT License"
          icon={Info}
        >
          <a href="https://github.com/broskifoo/Project-Beacon/blob/main/LICENSE" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">
            View License
          </a>
        </SettingItem>
        
        <SettingItem
          label="Report Issue"
          description="Report bugs or request features"
          icon={Info}
        >
          <a href="https://github.com/broskifoo/Project-Beacon/issues" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">
            GitHub Issues
          </a>
        </SettingItem>
      </SettingsSection>
    </div>
  )
}

function SettingsSection({ title, icon: Icon, children }: { title: string; icon: React.ComponentType<{ className?: string }>; children: React.ReactNode }) {
  return (
    <div className="bg-surface rounded-lg border border overflow-hidden">
      <div className="px-5 py-4 border-b border flex items-center gap-3">
        <Icon className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-semibold">{title}</h2>
      </div>
      <div className="p-4 space-y-4">
        {children}
      </div>
    </div>
  )
}

function SettingItem({ 
  label, 
  description, 
  icon: Icon, 
  children 
}: { 
  label: string
  description: string
  icon: React.ComponentType<{ className?: string }>
  children: React.ReactNode
}) {
  return (
    <div className="flex items-start gap-4">
      <div className="p-2 rounded-lg bg-surface-variant flex-shrink-0">
        <Icon className="h-5 w-5 text-text-secondary" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="font-medium">{label}</p>
            <p className="text-sm text-text-secondary mt-0.5">{description}</p>
          </div>
          <div className="flex-shrink-0">
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}

function Toggle({ checked, onChange, disabled }: { checked: boolean; onChange: (v: boolean) => void; disabled?: boolean }) {
  return (
    <button
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => !disabled && onChange(!checked)}
      className={`relative w-11 h-6 rounded-full transition-colors ${
        checked ? 'bg-primary' : 'bg-surface-variant'
      } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      <span className={`absolute top-0.5 transition-transform ${
        checked ? 'left-5' : 'left-0.5'
      } w-5 h-5 bg-white rounded-full shadow`} />
    </button>
  )
}

const Info = ({ className }: { className?: string }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
  </svg>
)