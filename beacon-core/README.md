# beacon-core

**Project Beacon Core** — Central orchestration, state management, and persistence layer.

## Overview

`beacon-core` is the central component of the Project Beacon platform. It provides:

- **Bundle Management**: DTN-style store-and-forward message bundles
- **Routing**: Multi-protocol routing (geographic, epidemic, direct)
- **Neighbor Management**: Peer discovery, tracking, and link quality
- **Persistence**: Encrypted SQLite storage (SQLCipher)
- **Scheduling**: Background task coordination
- **Topology**: Network topology maintenance and distribution

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON CORE                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │  BundleApi  │ │ RoutingApi  │ │ NeighborApi │               │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘               │
│         │               │               │                       │
│         └───────────────┼───────────────┘                       │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    CORE ENGINE                            │   │
│  │  • Bundle lifecycle (create, store, forward, deliver)   │   │
│  │  • Routing table management                             │   │
│  │  • Neighbor state machine                               │   │
│  │  • Custody transfer (DTN)                               │   │
│  │  • Topology dissemination                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                       │
│         ┌───────────────┼───────────────┐                       │
│         ▼               ▼               ▼                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │  Storage    │ │  Scheduler  │ │   Crypto    │               │
│  │  (SQLCipher)│ │  (WorkMgr)  │ │  (Identity) │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Components

| Module | Language | Purpose |
|--------|----------|---------|
| `kotlin` | Kotlin Multiplatform | Android/JVM API, platform integration |
| `rust/core-lib` | Rust | Core protocol logic, crypto, data structures |

## API

### Bundle Management
```kotlin
// Create and send a bundle
val bundleId = core.bundle.createBundle(
    destination = peerId,
    payload = encryptedPayload,
    priority = MessagePriority.HIGH,
    maxHops = 5
).getOrThrow()

// Observe delivery status
core.bundle.observeBundle(bundleId)
    .onEach { bundle ->
        when (bundle?.status) {
            BundleStatus.DELIVERED -> onDelivered()
            BundleStatus.ACKNOWLEDGED -> onAcked()
        }
    }
    .launchIn(scope)
```

### Routing
```kotlin
// Find route to destination
val route = core.routing.findRoute(destinationPeerId).getOrNull()

// Observe routing table changes
core.routing.observeRoutingTable()
    .onEach { table -> updateTopologyView(table) }
    .launchIn(scope)
```

### Neighbors
```kotlin
// Get active neighbors
val neighbors = core.neighbor.getActiveNeighbors().getOrDefault(emptyList())

// Observe neighbor state
core.neighbor.observeNeighbors()
    .onEach { peers -> updatePeerList(peers) }
    .launchIn(scope)
```

## Storage

Uses **SQLCipher** (AES-256-GCM) for encrypted at-rest storage:

```sql
-- Bundles table
CREATE TABLE bundles (
    id TEXT PRIMARY KEY,
    source TEXT NOT NULL,
    destination TEXT,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    payload BLOB NOT NULL,
    hop_count INTEGER DEFAULT 0,
    max_hops INTEGER DEFAULT 5,
    status INTEGER NOT NULL,
    custody_holder TEXT
);

-- Neighbors table
CREATE TABLE neighbors (
    peer_id TEXT PRIMARY KEY,
    display_name TEXT,
    last_seen INTEGER NOT NULL,
    location_x REAL,
    location_y REAL,
    battery INTEGER,
    power_mode INTEGER,
    transports TEXT,
    signal_strength INTEGER,
    trust_score REAL DEFAULT 0.0
);

-- Routing table
CREATE TABLE routes (
    destination TEXT PRIMARY KEY,
    next_hop TEXT NOT NULL,
    hop_count INTEGER NOT NULL,
    quality REAL NOT NULL,
    route_type INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

## Building

```bash
# Kotlin (Android/JVM)
./gradlew :beacon-core:kotlin:assemble

# Rust
cd rust/core-lib && cargo build --release
```

## Testing

```bash
# Unit tests
./gradlew :beacon-core:kotlin:test
cd rust/core-lib && cargo test

# Integration tests
./gradlew :beacon-core:kotlin:connectedAndroidTest
```

## Dependencies

- `beacon-sdk` — Public API interfaces
- `sqlcipher` — Encrypted storage
- `kotlinx-coroutines` — Async primitives
- `kotlinx-serialization` — CBOR/JSON serialization
- `kotlinx-datetime` — Time handling

## License

MIT License — see [LICENSE](../../LICENSE)