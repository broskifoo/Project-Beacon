import { Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { MapView } from './views/MapView'
import { MessagesView } from './views/MessagesView'
import { NetworkView } from './views/NetworkView'
import { ResourcesView } from './views/ResourcesView'
import { AlertsView } from './views/AlertsView'
import { SettingsView } from './views/SettingsView'
import { useBeaconStore } from './stores/beaconStore'

function App() {
  const { initialize } = useBeaconStore()
  
  React.useEffect(() => {
    initialize()
  }, [initialize])

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/map" replace />} />
        <Route path="map" element={<MapView />} />
        <Route path="messages" element={<MessagesView />} />
        <Route path="network" element={<NetworkView />} />
        <Route path="resources" element={<ResourcesView />} />
        <Route path="alerts" element={<AlertsView />} />
        <Route path="settings" element={<SettingsView />} />
      </Route>
    </Routes>
  )
}

export default App