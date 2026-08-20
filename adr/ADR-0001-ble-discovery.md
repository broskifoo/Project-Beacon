# ADR-0001: Primary Local Discovery Mechanism

## Document Metadata

* **Document ID:** `ADR-0001`
* **Version:** `1.0.0`
* **Status:** Accepted
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Status

**Accepted** — Bluetooth Low Energy (BLE) is the primary discovery and low-bandwidth transport mechanism for Phase 1-3. Wi-Fi Direct is the secondary high-bandwidth transport.

---

## Context

Project Beacon requires a mechanism for nearby devices to discover each other and exchange data without Internet connectivity. The discovery mechanism must:

1. **Operate without infrastructure** — No Wi-Fi APs, no cellular, no Internet
2. **Be energy-efficient** — Minimize battery drain during idle discovery
3. **Work on commodity Android** — No root, no special permissions beyond standard BLE
4. **Support background operation** — Discovery must work when app is not in foreground
5. **Scale to ~50 concurrent peers** — Dense urban / shelter scenarios
6. **Provide reasonable range** — 30-100m typical, up to 200m line-of-sight

### Candidate Technologies Evaluated

| Technology | Pros | Cons |
|------------|------|------|
| **Bluetooth Low Energy (BLE)** | Universal on Android/iOS; low power; background scanning supported; mature APIs; mesh profile exists | Limited bandwidth (~1 Mbps raw); connection limits (~7 concurrent); range limited |
| **Wi-Fi Direct (Wi-Fi P2P)** | High bandwidth (~250 Mbps); longer range (200m+); IP-based; supports groups | Higher power; no background scanning on Android; group owner negotiation complexity; not universal on iOS |
| **Bluetooth Mesh** | Standardized mesh; flooding-based; low power | Requires provisioning; not directly accessible on Android (no system API); limited phone-to-phone |
| **LoRa/Sub-GHz** | km-range; very low power | Requires external hardware; regulatory complexity; not on phones |
| **Ultrasonic/Audio** | Works on any speaker/mic | Very low bandwidth; unreliable; privacy concerns |
| **QR Code / NFC** | Zero power; simple | Requires user action; no background; proximity only |

---

## Decision

**Primary: Bluetooth Low Energy (BLE)**
- Used for: Peer discovery, presence, small messages (< 512 bytes), initial handshake
- Mode: Peripheral + Central simultaneously (dual role)
- Advertising: Interval 200-500ms (configurable by power mode)
- Scanning: Duty-cycled (10% duty cycle in normal mode)
- Connection: L2CAP CoC (Connection-Oriented Channels) for reliable streams, or GATT for simplicity

**Secondary: Wi-Fi Direct (Wi-Fi P2P)**
- Used for: Bulk transfer (maps, images, voice notes, large bundles)
- Triggered: On-demand when payload > 512 bytes or explicit user action
- Group Owner: Prefer device with higher battery / plugged in

**Future: LoRa via External Hardware**
- Abstracted behind `beacon-radio` transport interface
- Not part of phone-to-phone MVP

---

## Alternatives Considered

### Alternative A: Wi-Fi Direct Only
- **Rejection Rationale**: No background discovery on Android 10+; high power consumption makes always-on mesh impractical; group formation latency (2-5s) too high for opportunistic encounters

### Alternative B: Bluetooth Mesh Profile
- **Rejection Rationale**: Android lacks system-level Bluetooth Mesh API; would require implementing mesh stack in-app (complex); provisioning model doesn't fit ad-hoc disaster scenario

### Alternative C: Custom Wi-Fi Aware (NAN)
- **Rejection Rationale**: Limited device support (Android 8.0+, but hardware-dependent); no iOS support; still higher power than BLE for discovery

### Alternative D: Ultrasonic Beaconing
- **Rejection Rationale**: Unreliable in noisy environments; speaker/mic access privacy concerns; bandwidth far too low for any useful payload

---

## Consequences

### Positive Impact
- **Universal compatibility**: Works on all Android 5.0+ devices (~99%+)
- **Background operation**: `BluetoothLeScanner` with `PendingIntent` enables discovery when app killed
- **Low power**: ~1-3% battery/hour for continuous scanning at 10% duty cycle
- **Mature ecosystem**: Extensive documentation, libraries (RxBluetooth, Nordic DFU)
- **Dual-role**: Single radio serves as both advertiser and scanner

### Negative/Trade-offs
- **Connection limits**: Android limits concurrent GATT connections (~7); must use L2CAP CoC or connectionless for scale
- **Bandwidth ceiling**: ~100-200 KB/s practical throughput; insufficient for maps/images
- **Range variability**: 10-50m indoors, 100-200m outdoors; body blocking significant
- **iOS limitations**: Background advertising restricted; iOS peer requires separate implementation (Phase 6)
- **Scan response size**: Limited to 31 bytes; requires connection for full peer info

### Dependencies
- **Android 8.0+** (API 26) for `BluetoothLeAdvertiser` and `BluetoothLeScanner` with settings
- **Location permission** required for BLE scanning (Android 6.0+)
- **Foreground service** required for background scanning on Android 10+
- **beacon-radio** module must implement BLE transport driver

---

## Implementation Notes

### Advertising Payload (31 bytes)
```
Flags (3) | Service UUID (17) | Device Hash (8) | Battery/Mode (3)
```
- Service UUID: `0xBEAC` (custom 16-bit, registered)
- Device Hash: First 8 bytes of SHA-256(identity_pubkey)
- Battery/Mode: 1 byte battery %, 1 byte power mode, 1 byte reserved

### Scan Response (31 bytes)
```
Device Name (max 24) | Version (2) | Capabilities (5)
```

### Connection Strategy
1. **Discovery**: Passive scan → detect peer advertisement
2. **Handshake**: Initiate L2CAP CoC connection (PSM 0xBEAC)
3. **Auth**: Exchange ephemeral X25519 keys, verify signatures
4. **Session**: Encrypted channel for message exchange
5. **Teardown**: Close CoC, return to scanning

### Power Mode Parameters

| Mode | Scan Interval | Scan Window | Adv Interval | Connection Priority |
|------|---------------|-------------|--------------|---------------------|
| Normal | 2000ms | 200ms (10%) | 500ms | HIGH |
| Conservation | 5000ms | 250ms (5%) | 1000ms | BALANCED |
| Survival | 30000ms | 500ms (1.6%) | 2000ms | LOW |
| Critical | 60000ms | 1000ms (1.6%) | 5000ms | LOW |

---

## References

* [Android BLE Developer Guide](https://developer.android.com/guide/topics/connectivity/bluetooth-le)
* [Bluetooth Core Specification v5.3](https://www.bluetooth.com/specifications/bluetooth-core-specification/)
* [ADR-0002: Offline GIS Database Engine Selection](ADR-0002-gis-database.md)
* [Security Specification](../security/security.md)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 1.0.0 | Initial decision | Project Beacon Core Team |

---

## Approval

**Status: ACCEPTED** ✅