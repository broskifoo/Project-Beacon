# beacon-sdk

**Project Beacon Software Development Kit** — Unified APIs for building disaster-resilient applications.

## Overview

The Beacon SDK provides a Kotlin Multiplatform library for integrating with the Project Beacon platform. It abstracts the underlying mesh networking, offline messaging, maps, and identity management behind clean, platform-agnostic interfaces.

## Features

- **Messaging**: Offline-first, priority-queued, end-to-end encrypted messaging
- **Peer Discovery**: BLE/Wi-Fi Direct/LoRa peer detection and presence
- **Mesh Networking**: Multi-hop routing, store-and-forward, network topology
- **Offline Maps**: Vector tiles, POI search, routing (Valhalla/OSRM)
- **Identity**: Ed25519 keys in Android Keystore / Secure Enclave
- **Power Management**: Battery-aware modes (Normal → Conservation → Survival → Critical)
- **Storage**: Encrypted at-rest storage with SQLCipher-compatible API

## Platform Support

| Platform | Status |
|----------|--------|
| Android (API 24+) | ✅ Primary |
| JVM (Desktop/Server) | ✅ |
| iOS (Arm64/Simulator) | 🔄 Planned |
| JavaScript (Web/Node) | 🔄 Planned |

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.beacon:beacon-sdk:0.1.0-alpha")
}
```

### Maven

```xml
<dependency>
    <groupId>org.beacon</groupId>
    <artifactId>beacon-sdk</artifactId>
    <version>0.1.0-alpha</version>
</dependency>
```

## Quick Start

```kotlin
// Android Application class
class BeaconApp : Application() {
    private lateinit var beaconSdk: BeaconSdk
    
    override fun onCreate() {
        super.onCreate()
        
        val config = BeaconConfig(
            deviceName = "My Beacon Node",
            enableBle = true,
            enableWifiDirect = true,
            powerMode = PowerMode.NORMAL
        )
        
        BeaconSdkFactory.create(this, config)
            .onSuccess { sdk ->
                beaconSdk = sdk
                sdk.initialize()
            }
            .onFailure { error ->
                Log.e("Beacon", "Failed to initialize: ${error.message}")
            }
    }
}

// Sending a message
val messageId = beaconSdk.messaging.sendMessage(
    recipientId = peerId,  // null for broadcast
    payload = MessagePayload(
        type = MessageType.TEXT,
        text = "Hello, mesh!"
    ),
    priority = MessagePriority.NORMAL
).getOrThrow()

// Observing incoming messages
beaconSdk.messaging.observeIncomingMessages()
    .onEach { message ->
        // Handle message
    }
    .launchIn(scope)

// Observing nearby peers
beaconSdk.peers.observePeers()
    .onEach { peers ->
        // Update UI
    }
    .launchIn(scope)
```

## Architecture

```
┌─────────────────────────────────────────────┐
│              BeaconSdk (Facade)             │
├─────────────────────────────────────────────┤
│  MessagingApi  │  PeerApi  │  NetworkApi   │
│  MapsApi       │  Identity │  PowerApi     │
│  StorageApi    │  Lifecycle                 │
└─────────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
    Android      JVM          iOS
  Implementation Implementation (planned)
```

## API Reference

### Core Interfaces

| Interface | Description |
|-----------|-------------|
| `MessagingApi` | Send/receive messages, SOS, location, resource reports |
| `PeerApi` | Peer discovery, presence, trust management |
| `NetworkApi` | Mesh topology, routing, network statistics |
| `MapsApi` | Offline vector tiles, POI search, routing |
| `IdentityApi` | Key generation, signing, ECDH key agreement |
| `PowerApi` | Battery monitoring, power modes, wake locks |
| `StorageApi` | Encrypted key-value storage |
| `LifecycleApi` | SDK initialization, health checks |

### Data Models

| Model | Description |
|-------|-------------|
| `Message` | Encrypted, signed mesh message with priority/TTL |
| `Peer` | Discovered mesh node with signal, battery, location |
| `Location` | GPS coordinates with accuracy/timestamp |
| `ResourceReport` | Community-reported resources (water, medical, etc.) |
| `Alert` | Broadcast alerts with geographic scope |
| `Poi` | Points of interest for offline maps |

## Building

```bash
./gradlew :kotlin:assemble
```

## Testing

```bash
./gradlew :kotlin:test
```

## License

MIT License — see [LICENSE](../../LICENSE)

## Contributing

See [CONTRIBUTING.md](../../CONTRIBUTING.md)