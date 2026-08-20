# SDK Specification

## Document Metadata

* **Document ID:** `DOC-SDK-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the Software Development Kit (SDK) for Project Beacon, enabling third-party developers to build applications that integrate with the Beacon mesh network.

---

## SDK Overview

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        BEACON SDK                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │   Kotlin    │ │   Swift     │ │ TypeScript  │  ...          │
│  │  (Android/  │ │  (iOS/      │ │  (Web/      │               │
│  │   JVM)      │ │   macOS)    │ │   Node.js)  │               │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘               │
│         │               │               │                       │
│         └───────────────┼───────────────┘                       │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    CORE (Rust)                           │   │
│  │  • Protocol Implementation (CBOR, COBS, Crypto)        │   │
│  │  • Mesh Routing (Hybrid, Epidemic, Geographic)         │   │
│  │  • Identity Management (Ed25519, X25519, HKDF)         │   │
│  │  • Storage Abstraction (SQLCipher, Key-Value)          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                       │
│         ┌───────────────┼───────────────┐                       │
│         ▼               ▼               ▼                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │  Android    │ │    iOS      │ │  Desktop/   │               │
│  │  (BLE/WiFi) │ │ (BLE/WiFi)  │ │  Server     │               │
│  │  Transport  │ │  Transport  │ │  (TCP/WS)   │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Language Support Matrix

| Language | Platform | Status | Distribution |
|----------|----------|--------|--------------|
| **Kotlin** | Android, JVM, JS, Native | ✅ Primary | Maven Central |
| **Swift** | iOS, macOS, watchOS | 🔄 Planned | CocoaPods / SPM |
| **TypeScript** | Browser, Node.js, React Native | 🔄 Planned | npm |
| **Python** | Desktop, Server, RPi | 🔄 Planned | PyPI |
| **Rust** | All (native) | 🔄 Planned | crates.io |
| **C/C++** | Embedded, Native | 🔄 Planned | Static lib |
| **Go** | Server, CLI | 🔄 Planned | Go modules |

---

## Core API (Kotlin Multiplatform)

### Initialization

```kotlin
// Android Application
class BeaconApp : Application() {
    private lateinit var beaconSdk: BeaconSdk
    
    override fun onCreate() {
        super.onCreate()
        
        val config = BeaconConfig(
            deviceName = "My Beacon App",
            enableBle = true,
            enableWifiDirect = true,
            powerMode = PowerMode.NORMAL,
            storagePath = filesDir.absolutePath
        )
        
        BeaconSdkFactory.create(this, config)
            .onSuccess { sdk ->
                beaconSdk = sdk
                sdk.initialize()
            }
            .onFailure { error ->
                Log.e("Beacon", "Init failed: ${error.message}")
            }
    }
}
```

### Messaging API

```kotlin
// Send a text message
val messageId = beaconSdk.messaging.sendMessage(
    recipientId = peerId,  // null = broadcast
    payload = MessagePayload(
        type = MessageType.TEXT,
        text = "Hello, mesh network!"
    ),
    priority = MessagePriority.NORMAL,
    ttl = 5
).getOrThrow()

// Send SOS (CRITICAL priority, auto-retry)
val sosId = beaconSdk.messaging.sendSos(
    location = currentLocation,
    customMessage = "Trapped on 3rd floor"
).getOrThrow()

// Send location update
beaconSdk.messaging.sendLocation(
    recipientId = teamLeadId,
    location = currentLocation
)

// Observe incoming messages
beaconSdk.messaging.observeIncomingMessages()
    .onEach { message ->
        when (message.payload.type) {
            MessageType.TEXT -> showChatMessage(message)
            MessageType.SOS -> triggerSosAlert(message)
            MessageType.LOCATION -> updatePeerLocation(message)
        }
    }
    .launchIn(scope)

// Track delivery status
beaconSdk.messaging.observeMessageStatus(messageId)
    .onEach { status ->
        updateUi(status.state)
    }
    .launchIn(scope)
```

### Peer Discovery API

```kotlin
// Observe nearby peers
beaconSdk.peers.observePeers()
    .onEach { peers ->
        adapter.submitList(peers.filter { it.isOnline() })
    }
    .launchIn(scope)

// Peer lifecycle events
beaconSdk.peers.observePeerEvents()
    .onEach { event ->
        when (event.eventType) {
            DiscoveryEventType.DISCOVERED -> showNewPeerNotification(event.peer)
            DiscoveryEventType.LOST -> removePeerFromUi(event.peer.id)
        }
    }
    .launchIn(scope)

// Trust management
beaconSdk.peers.trustPeer(peerId, true)
beaconSdk.peers.blockPeer(maliciousPeerId)
```

### Network Topology API

```kotlin
// Real-time topology
beaconSdk.network.observeTopology()
    .onEach { topology ->
        renderMeshGraph(topology.nodes, topology.edges)
    }
    .launchIn(scope)

// Route to specific peer
val route = beaconSdk.network.getRouteTo(targetPeerId).getOrNull()
route?.let { showRoute(it.path) }

// Network statistics
beaconSdk.network.observeNetworkStats()
    .onEach { stats ->
        updateDashboard(stats)
    }
    .launchIn(scope)
```

### Maps API

```kotlin
// Search POIs
val hospitals = beaconSdk.maps.searchPoi(
    query = "hospital",
    category = ResourceType.MEDICAL,
    bounds = currentMapBounds,
    limit = 20
).getOrDefault(emptyList())

// Nearby resources
val nearby = beaconSdk.maps.getNearbyPoi(
    location = currentLocation,
    radiusMeters = 5000.0,
    category = ResourceType.WATER
).getOrDefault(emptyList())

// Calculate route
val route = beaconSdk.maps.calculateRoute(
    from = currentLocation,
    to = destination,
    profile = RoutingProfile.FOOT
).getOrNull()

// Observe community marker updates
beaconSdk.maps.observePoiUpdates()
    .onEach { update ->
        refreshMarker(update.poi, update.updateType)
    }
    .launchIn(scope)
```

### Identity API

```kotlin
// Get local identity
val identity = beaconSdk.identity.getIdentity().getOrThrow()
val publicKey = identity.publicKey  // Ed25519 hex
val keyId = identity.keyId  // First 16 chars

// Sign data
val signature = beaconSdk.identity.sign(data.toByteArray()).getOrThrow()

// Verify signature
val valid = beaconSdk.identity.verifySignature(
    data = data.toByteArray(),
    signature = signatureHex.hexToByteArray(),
    publicKey = peerPublicKey
).getOrDefault(false)

// Derive shared secret for encryption
val sharedSecret = beaconSdk.identity.deriveSharedSecret(peerPublicKey).getOrThrow()
```

### Power Management API

```kotlin
// Observe power mode
beaconSdk.power.observePowerMode()
    .onEach { mode ->
        updateUiForPowerMode(mode)
    }
    .launchIn(scope)

// Battery level
beaconSdk.power.observeBatteryLevel()
    .onEach { level ->
        batteryView.setLevel(level)
    }
    .launchIn(scope)

// Request wake lock for critical operation
val token = beaconSdk.power.requestWakeLock(30_000).getOrThrow() // 30s
try {
    // Critical network operation
} finally {
    beaconSdk.power.releaseWakeLock(token)
}
```

### Storage API

```kotlin
// Encrypted key-value storage
beaconSdk.storage.putEncrypted("user_prefs", prefsJson.toByteArray())
val prefs = beaconSdk.storage.getEncrypted("user_prefs")
    .getOrNull()?.let { String(it) }?.let { JSONObject(it) }

// Regular storage (non-encrypted)
beaconSdk.storage.put("cache_key", cacheData)
val data = beaconSdk.storage.get("cache_key")
```

---

## Platform-Specific APIs

### Android Extensions

```kotlin
// Foreground service management
beaconSdk.lifecycle.observeState()
    .onEach { state ->
        when (state) {
            LifecycleState.STARTING -> showStartingNotification()
            LifecycleState.RUNNING -> updateForegroundNotification()
            LifecycleState.STOPPED -> stopForegroundService()
        }
    }
    .launchIn(scope)

// Bluetooth permission handling (Android 12+)
val permResult = beaconSdk.android.requestBluetoothPermissions()
```

### Desktop/Server Extensions

```kotlin
// TCP/WebSocket transport for gateway nodes
val gatewayConfig = GatewayConfig(
    listenAddress = "0.0.0.0:4242",
    enableWebSocket = true,
    enableMetrics = true
)
beaconSdk.gateway.start(gatewayConfig)
```

---

## Plugin Architecture

### Transport Plugins

```kotlin
interface TransportPlugin {
    val transportType: TransportType
    fun initialize(config: TransportConfig): Result<Unit>
    fun send(peerId: PeerId, frame: BeaconFrame): Result<Unit>
    fun receive(): Flow<BeaconFrame>
    fun shutdown(): Result<Unit>
}

// Register custom transport
beaconSdk.registerTransport(CustomLoRaTransport())
```

### Message Handlers

```kotlin
// Custom message type handler
beaconSdk.messaging.registerHandler(MessageType.CUSTOM) { message ->
    if (message.payload.custom?.get("app_id") == "my_app") {
        handleCustomMessage(message)
    }
}
```

---

## Distribution & Versioning

### Maven (Kotlin/JVM/Android)

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.beacon:beacon-sdk:0.1.0-alpha")
    implementation("org.beacon:beacon-sdk-android:0.1.0-alpha")
}
```

### CocoaPods (Swift)

```ruby
# Podfile
pod 'BeaconSDK', '~> 0.1.0-alpha'
```

### npm (TypeScript)

```json
{
  "dependencies": {
    "@beacon/sdk": "^0.1.0-alpha"
  }
}
```

### PyPI (Python)

```bash
pip install beacon-sdk==0.1.0-alpha
```

### Versioning Policy

- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Pre-1.0**: Minor versions may have breaking changes
- **Stability**: API marked `@Experimental` may change
- **Deprecation**: 2 minor versions notice before removal

---

## Security Considerations

| Aspect | Guidance |
|--------|----------|
| **Key Storage** | Use platform secure enclave (Keystore, Secure Enclave, TPM) |
| **Network** | All traffic encrypted by default; no plaintext option |
| **Permissions** | Request minimum permissions; explain rationale |
| **Data Export** | User-controlled; encrypted exports only |
| **Audit** | Log security events locally; no remote telemetry |

---

## Testing

### Mock SDK for Unit Tests

```kotlin
@Test
fun `message sent and delivered`() {
    val mockSdk = MockBeaconSdk()
    
    val messageId = mockSdk.messaging.sendMessage(
        recipientId = PeerId("test"),
        payload = MessagePayload(type = MessageType.TEXT, text = "test")
    ).getOrThrow()
    
    // Simulate delivery
    mockSdk.simulateDelivery(messageId)
    
    assertEquals(DeliveryState.ACKNOWLEDGED, 
        mockSdk.messaging.getMessageStatus(messageId).state)
}
```

### Integration Test Harness

```kotlin
// beacon-sdk-test-harness artifact
@BeaconIntegrationTest
class MeshIntegrationTest {
    
    @Test
    fun `two nodes exchange messages`() {
        val nodeA = TestNode.create()
        val nodeB = TestNode.create()
        
        nodeA.connect(nodeB)
        
        nodeA.sendMessage(nodeB.id, "Hello")
        
        await().untilAsserted {
            assertTrue(nodeB.hasReceived("Hello"))
        }
    }
}
```

---

## Migration Guide (Future)

### v0.x → v1.0

| Change | Action |
|--------|--------|
| `BeaconSdk.create()` | → `BeaconSdkFactory.create(context, config)` |
| `Message.payload.text` | → `Message.payload.content` (sealed class) |
| `PeerApi.observePeers()` | → Returns `StateFlow<List<Peer>>` |
| `PowerMode` enum | → Renamed to `PowerState` |

---

## References

* [Architecture Overview](../architecture/architecture.md)
* [Protocol Specification](../protocol/protocol.md)
* [Security Specification](../security/security.md)
* [beacon-sdk Repository](https://github.com/broskifoo/Project-Beacon/tree/main/beacon-sdk)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |