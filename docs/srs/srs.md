# Software Requirements Specification (SRS)

## Document Metadata

* **Document ID:** `DOC-SRS-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This SRS specifies the detailed software requirements for the Project Beacon platform, derived from the PRD (`DOC-PRD-001`) and Architecture (`DOC-ARCH-001`). It serves as the authoritative reference for implementation and testing.

---

## System Overview

### System Context

```
┌─────────────────────────────────────────────────────────────────┐
│                        BEACON PLATFORM                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Android    │    │   Android    │    │  Beacon      │      │
│  │   Phone A    │◄───►│   Phone B    │◄───►│  Radio Node  │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│        │                   │                   │                 │
│        └───────────────────┼───────────────────┘                 │
│                            ▼                                     │
│                   ┌─────────────────┐                            │
│                   │  Mesh Network   │                            │
│                   │  (Store-Forward)│                            │
│                   └─────────────────┘                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Product Functions Summary

| Function | Description | Priority |
|----------|-------------|----------|
| **Offline Messaging** | Send/receive text, location, SOS without Internet | MUST |
| **Peer Discovery** | Automatic BLE/Wi-Fi peer detection | MUST |
| **Mesh Routing** | Multi-hop, store-and-forward | MUST |
| **Encryption** | E2E encryption, signatures, forward secrecy | MUST |
| **Offline Maps** | Vector tiles, POI search, routing | SHOULD |
| **Resource Sharing** | Community reports (water, medical, shelter) | SHOULD |
| **Power Management** | Battery-aware duty cycling | MUST |
| **Local AI** | Offline summarization, translation | COULD |

---

## Functional Requirements

### FR-1: Messaging Subsystem

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-1.1** | Send Message | Support unicast and broadcast; priority queue (CRITICAL/HIGH/NORMAL/LOW); TTL 1-255 hops |
| **FR-1.2** | Receive Message | Decrypt, verify signature, deduplicate, persist, notify UI |
| **FR-1.3** | Message Types | TEXT, LOCATION, TELEMETRY, SOS, ACK, RESOURCE_REPORT, ALERT, MAP_TILE, VOICE, IMAGE, CUSTOM |
| **FR-1.4** | Delivery Status | QUEUED → SENDING → SENT → DELIVERED → ACKNOWLEDGED; FAILED/EXPIRED terminal states |
| **FR-1.5** | Retry Logic | Exponential backoff (1s, 2s, 4s, 8s, 16s, max 60s); priority-aware |
| **FR-1.6** | Fragmentation | Auto-fragment > MTU; reassembly timeout 30s; per-fragment ACK |
| **FR-1.7** | Message History | Local SQLite storage; search by peer, type, time; configurable retention (default 30 days) |
| **FR-1.8** | SOS Handling | Special CRITICAL priority; auto-retry until ACK; lock-screen activation; location + battery + ID |

### FR-2: Peer Discovery & Management

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-2.1** | BLE Advertising | Interval 200-5000ms (power-mode dependent); 31-byte adv + 31-byte scan response |
| **FR-2.2** | BLE Scanning | Duty-cycled (10% normal, 5% conservation, 1.6% survival); passive + active |
| **FR-2.3** | Wi-Fi Direct | On-demand for bulk transfer; Group Owner preference by battery/plugged |
| **FR-2.4** | Peer State Machine | DISCOVERED → CONNECTING → CONNECTED → ACTIVE → DEGRADED → LOST |
| **FR-2.5** | Neighbor Table | Track: peer ID, signal (RSSI), battery, power mode, transports, last seen, trust |
| **FR-2.6** | Trust Management | TOFU model; user verification; trust scores (0.0-1.0); block/unblock |

### FR-3: Mesh Networking

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-3.1** | Routing Protocol | Hybrid: Geographic (known dest) + Epidemic (unknown); probabilistic forwarding |
| **FR-3.2** | Store-and-Forward | Persist bundles to SQLite; forward on contact; max buffer 1000 messages |
| **FR-3.3** | TTL/Hop Limit | Decrement TTL per hop; drop at 0; default TTL=5, max 255 |
| **FR-3.4** | Duplicate Detection | Bloom filter (10k entries) + persistent message ID store |
| **FR-3.5** | Priority Forwarding | CRITICAL always forwarded; HIGH 2x probability; LOW 0.1x |
| **FR-3.6** | Topology Awareness | Periodic topology broadcast (every 30s); on-demand route discovery |

### FR-4: Cryptography & Identity

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-4.1** | Identity Keys | Ed25519 generated in Secure Element (Android Keystore / iOS Secure Enclave) |
| **FR-4.2** | Session Keys | X25519 ECDH per session; HKDF-SHA256 derivation; forward secrecy |
| **FR-4.3** | Message Encryption | ChaCha20-Poly1305; 96-bit nonce (UUIDv7 + counter); AAD = routing header |
| **FR-4.4** | Signatures | Ed25519 over routing header || ciphertext; verified on every receive |
| **FR-4.5** | Key Rotation | Automatic 90-day rotation; cross-sign old→new; revocation via mesh |
| **FR-4.6** | Secure Storage | SQLCipher AES-256-GCM; key wrapped by user passphrase + hardware key |

### FR-5: Power Management

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-5.1** | Power Modes | NORMAL (>50%), CONSERVATION (20-50%), SURVIVAL (10-20%), CRITICAL (<10%) |
| **FR-5.2** | Duty Cycling | Scan interval/window per mode (see ADR-0001); advertise interval per mode |
| **FR-5.3** | Batch Processing | Queue messages; single radio wake per batch; max batch 10 messages |
| **FR-5.4** | Battery Reporting | Include battery % in telemetry; share with neighbors for energy-aware routing |
| **FR-5.5** | Wake Locks | Acquire for TX/RX; timeout-based release; partial wake lock only |
| **FR-5.6** | Background Execution | Foreground Service with notification; WorkManager for periodic tasks |

### FR-6: Offline Maps & Navigation

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-6.1** | Vector Tiles | MBTiles format (SQLite); MapLibre rendering; zoom 0-16 |
| **FR-6.2** | POI Database | SpatiaLite (SQLite + R-tree); categories: water, medical, shelter, food, hazard, charging |
| **FR-6.3** | POI Search | Text search (FTS5); category filter; bbox filter; radius search; <100ms |
| **FR-6.4** | Routing | Valhalla/OSRM offline; profiles: foot, bicycle, car, wheelchair; GPX export |
| **FR-6.5** | Community Markers | CRDT sync via mesh; LWW-Register with version vectors; confidence scoring |
| **FR-6.6** | Map Updates | Mesh sync + manual USB/sideload; differential updates; signature verification |

### FR-7: Local AI (Phase 6+)

| ID | Requirement | Details |
|----|-------------|---------|
| **FR-7.1** | Model Runtime | ONNX Runtime / ExecuTorch / llama.cpp; quantized (INT4/INT8) |
| **FR-7.2** | Capabilities | Summarization, translation, procedure lookup, triage assistance |
| **FR-7.3** | Knowledge Base | Offline Wikipedia medical subset, first aid guides, survival manuals |
| **FR-7.4** | Non-Critical | Networking MUST work without AI; AI disabled in SURVIVAL/CRITICAL modes |

---

## Non-Functional Requirements

### NFR-1: Performance

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| **NFR-1.1** | Message latency (direct) | < 5s p95 | End-to-end timestamp |
| **NFR-1.2** | Message latency (3-hop) | < 30s p95 | End-to-end timestamp |
| **NFR-1.3** | Delivery rate (direct) | > 95% | Simulated + field test |
| **NFR-1.4** | Delivery rate (3-hop) | > 80% | Simulated + field test |
| **NFR-1.5** | Map render FPS | 60 fps | Frame timing |
| **NFR-1.6** | POI search latency | < 100ms p95 | Local query benchmark |
| **NFR-1.7** | App cold start | < 3s | Android vitals |
| **NFR-1.8** | Background CPU | < 5% average | Battery historian |

### NFR-2: Reliability

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| **NFR-2.1** | Crash-free sessions | > 99.9% | Play Console |
| **NFR-2.2** | ANR rate | < 0.1% | Play Console |
| **NFR-2.3** | Message persistence | 100% | Power-cut test |
| **NFR-2.4** | Database corruption | 0 | Stress test + recovery |
| **NFR-2.5** | Upgrade compatibility | Seamless | Migration tests |

### NFR-3: Security

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| **NFR-3.1** | Encryption coverage | 100% of messages | Code audit |
| **NFR-3.2** | Key storage | Hardware-backed | Keystore attestation |
| **NFR-3.3** | Forward secrecy | Per-session | Protocol verification |
| **NFR-3.4** | Vulnerability density | 0 critical/high | SAST/DAST/pen test |

### NFR-4: Power Efficiency

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| **NFR-4.1** | Idle drain (NORMAL) | < 5%/hour | 24h test, screen off |
| **NFR-4.2** | Idle drain (CONSERVATION) | < 2%/hour | 24h test |
| **NFR-4.3** | Idle drain (SURVIVAL) | < 0.5%/hour | 24h test |
| **NFR-4.4** | Active messaging | < 15%/hour | Continuous messaging test |
| **NFR-4.5** | Wake lock duration | < 100ms per batch | Trace analysis |

### NFR-5: Usability

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| **NFR-5.1** | SOS activation time | < 2s, 3 taps | Usability test |
| **NFR-5.2** | Offline map load | < 2s | Cold start |
| **NFR-5.3** | Accessibility | TalkBack/VoiceOver | Audit |
| **NFR-5.4** | Localization | 10+ languages | Crowdsourced |

---

## Interface Requirements

### IF-1: Android Platform

| Interface | Specification |
|-----------|---------------|
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 34 (Android 14) |
| **Permissions** | BLUETOOTH_CONNECT, BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, WIFI_DIRECT, FOREGROUND_SERVICE, WAKE_LOCK |
| **Architecture** | arm64-v8a, armeabi-v7a |
| **Storage** | Scoped storage (app-specific) + MediaStore for exports |

### IF-2: External Radio (Bluetooth Serial)

| Interface | Specification |
|-----------|---------------|
| **Transport** | Bluetooth Classic SPP / BLE GATT |
| **Baud Rate** | 115200 / 921600 |
| **Protocol** | COBS framing + CRC16 + Beacon Frame |
| **Commands** | AT-style: `AT+SEND`, `AT+RECV`, `AT+CONFIG`, `AT+STATUS` |

### IF-3: SDK (Public API)

| Interface | Specification |
|-----------|---------------|
| **Languages** | Kotlin (Android/JVM), Swift (iOS), TypeScript (Web), Python, Rust |
| **Distribution** | Maven Central, CocoaPods, npm, PyPI, crates.io |
| **Versioning** | Semantic Versioning; stable API v1.0+ |

### IF-4: Map Data Import

| Format | Tool | Notes |
|--------|------|-------|
| **OSM PBF** | planetiler → MBTiles | Primary source |
| **GeoJSON** | tippecanoe → MBTiles | Community overlays |
| **MBTiles** | Direct import | Pre-built tiles |
| **GeoPackage** | ogr2ogr → SpatiaLite | Vector features |

---

## Data Requirements

### DR-1: Local Database Schema

```sql
-- Messages table
CREATE TABLE messages (
    id TEXT PRIMARY KEY,              -- UUIDv7
    sender_id TEXT NOT NULL,          -- Peer ID (Ed25519 pubkey)
    recipient_id TEXT,                -- NULL = broadcast
    timestamp INTEGER NOT NULL,       -- Unix ms
    priority INTEGER NOT NULL,        -- 0=LOW, 1=NORMAL, 2=HIGH, 3=CRITICAL
    ttl INTEGER NOT NULL DEFAULT 5,
    hop_count INTEGER NOT NULL DEFAULT 0,
    payload BLOB NOT NULL,            -- Encrypted
    signature BLOB NOT NULL,          -- Ed25519
    nonce BLOB NOT NULL,              -- 12 bytes
    status INTEGER NOT NULL,          -- Enum: QUEUED, SENT, DELIVERED, ACKED, FAILED, EXPIRED
    created_at INTEGER NOT NULL,
    delivered_at INTEGER,
    acked_at INTEGER
);

-- Peers table
CREATE TABLE peers (
    pubkey TEXT PRIMARY KEY,          -- Ed25519 (hex)
    display_name TEXT,
    last_seen INTEGER NOT NULL,
    location_x REAL,                  -- WGS84 longitude
    location_y REAL,                  -- WGS84 latitude
    battery INTEGER,                  -- 0-100
    power_mode INTEGER,               -- Enum
    transports TEXT,                  -- JSON array
    trust_score REAL DEFAULT 0.0,
    is_blocked INTEGER DEFAULT 0,
    version_vector TEXT               -- JSON CRDT version vector
);

-- POIs table (SpatiaLite)
CREATE TABLE pois (
    id TEXT PRIMARY KEY,
    source TEXT NOT NULL,             -- 'osm', 'community', 'official'
    category TEXT NOT NULL,
    name TEXT,
    description TEXT,
    geometry POINT NOT NULL,
    properties TEXT,                  -- JSON
    confidence REAL DEFAULT 1.0,
    version_vector TEXT NOT NULL,     -- JSON CRDT
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    expires_at INTEGER,
    reporter_pubkey TEXT,
    signature BLOB
);
```

### DR-2: Data Retention

| Data Type | Retention | Cleanup |
|-----------|-----------|---------|
| Messages (delivered) | 30 days | Background job |
| Messages (failed) | 7 days | Background job |
| Peer records | 7 days since last seen | On startup |
| POIs (OSM) | Permanent | Manual update |
| POIs (community) | 24h default, configurable | Expiry timestamp |
| Telemetry | 1 hour | Rolling window |
| Logs | 10k entries / 100MB | Circular buffer |

---

## Operational Requirements

### OR-1: Deployment

| Requirement | Specification |
|-------------|---------------|
| **Distribution** | Google Play Store, F-Droid, GitHub Releases, direct APK |
| **Updates** | In-app update API (Play Core) + mesh OTA (future) |
| **Configuration** | Encrypted JSON in app-private storage; schema versioned |
| **Logging** | Structured JSON; local only; no telemetry without consent |

### OR-2: Monitoring & Diagnostics

| Requirement | Specification |
|-------------|---------------|
| **Health Checks** | Bluetooth, Storage, Battery, Mesh connectivity, Crypto |
| **Metrics** | Local Prometheus endpoint (`/metrics`); exported to dashboard |
| **Debug Logs** | User-triggered log package (encrypted, user-controlled) |
| **Crash Reporting** | Opt-in; local only unless user shares |

### OR-3: Accessibility

| Requirement | Specification |
|-------------|---------------|
| **Screen Readers** | TalkBack (Android), VoiceOver (iOS) compatible |
| **Contrast** | WCAG 2.1 AA minimum |
| **Touch Targets** | 48x48dp minimum |
| **Language** | RTL support; system locale |

---

## Verification & Validation

### Test Categories

| Category | Coverage Target | Tools |
|----------|-----------------|-------|
| **Unit Tests** | > 80% line coverage | JUnit, MockK, Robolectric |
| **Integration Tests** | All API surfaces | AndroidTest, Espresso |
| **Simulation Tests** | All routing protocols | beacon-simulator (10-10k nodes) |
| **Field Tests** | 10+ devices, urban/rural | Automated test harness |
| **Battery Tests** | 24h per power mode | Battery Historian |
| **Security Tests** | Crypto validation, fuzzing | Wycheproof, AFL++ |
| **Interop Tests** | Multiple Android versions | Device farm (Firebase Test Lab) |

### Acceptance Criteria for Milestone 2 (Communication MVP)

- [ ] Two Android phones exchange messages via BLE (Internet OFF)
- [ ] Message persistence survives app kill / reboot
- [ ] Delivery status updates correctly (SENT → DELIVERED → ACKED)
- [ ] SOS message sends with location, triggers notification on peer
- [ ] Battery drain < 5%/hour idle (NORMAL mode)
- [ ] App runs 24h without crash in background

---

## Appendix: Requirement Traceability

| SRS Requirement | PRD Requirement | ADR | Test Case |
|-----------------|-----------------|-----|-----------|
| FR-1.1 → FR-1.8 | REQ-COMM-001 → 009 | — | TC-MSG-001 → 008 |
| FR-2.1 → FR-2.6 | REQ-COMM-001, 003 | ADR-0001 | TC-DISC-001 → 006 |
| FR-3.1 → FR-3.6 | REQ-MESH-001 → 003 | — | TC-MESH-001 → 006 |
| FR-4.1 → FR-4.6 | REQ-SEC-001 → 003 | — | TC-CRYPTO-001 → 006 |
| FR-5.1 → FR-5.6 | REQ-POWER-001 | — | TC-POWER-001 → 006 |
| FR-6.1 → FR-6.6 | REQ-MAP-001 → 004 | ADR-0002 | TC-MAP-001 → 006 |

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |