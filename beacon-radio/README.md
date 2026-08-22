# beacon-radio

**Project Beacon Radio Layer** — Transport abstraction and drivers for BLE, Wi-Fi Direct, and LoRa.

## Overview

`beacon-radio` provides a unified transport abstraction layer that allows the mesh networking layer to send and receive frames over multiple radio technologies without knowing the underlying implementation details.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON RADIO                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              TransportApi (Interface)                    │    │
│  │  • send(peerId, frame)                                   │    │
│  │  • receive(): Channel<BeaconFrame>                       │    │
│  │  • observeLinkQuality(peerId)                            │    │
│  │  • observePeerEvents()                                   │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                           │
│         ┌───────────┼───────────┐                               │
│         ▼           ▼           ▼                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │   BLE       │  │  Wi-Fi      │  │   LoRa      │                │
│  │  Transport  │  │  Direct     │  │  Transport  │                │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘                │
│         │               │               │                         │
│         ▼               ▼               ▼                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │  Android    │  │  Android    │  │  External   │                │
│  │  BLE APIs   │  │  Wi-Fi P2P  │  │  Radio UART │                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Transport Comparison

| Feature | BLE | Wi-Fi Direct | LoRa (External) |
|---------|-----|--------------|-----------------|
| **Range** | 30-100m | 100-200m | 1-15 km |
| **Bandwidth** | ~1 Mbps | ~50 Mbps | ~5 kbps |
| **Latency** | 10-50ms | 5-20ms | 100-500ms |
| **Power** | Very Low | Medium | Low |
| **Background** | ✅ Yes | ⚠️ Limited | ✅ Yes |
| **Fragmentation** | ✅ Required | ✅ Optional | ✅ Required |
| **Max Payload** | 512 bytes | 64 KB | 255 bytes |

## Usage

```kotlin
// Configure transports
val radioConfig = RadioConfig(
    enableBle = true,
    enableWifiDirect = true,
    enableLora = false,
    bleConfig = TransportConfig(
        scanIntervalMs = 2000,
        scanWindowMs = 200,
        advertiseIntervalMs = 500
    )
)

// Initialize RadioManager
val radioManager = RadioManager(context, radioConfig, meshEngine)

// Start all transports
radioManager.start()

// Send frame
radioManager.sendToAll(frame)
```

## BLE Transport

- **Primary transport** for peer discovery and messaging
- Uses L2CAP CoC (Connection-Oriented Channels) for reliable streams
- Background scanning supported via `BluetoothLeScanner` with `PendingIntent`
- Automatic connection management with retry logic
- Link quality monitoring (RSSI, SNR, packet loss)

### BLE Service UUID
- Service: `0000BEAC-0000-1000-8000-00805F9B34FB`
- TX Characteristic: `0000BEAC-0001-1000-8000-00805F9B34FB`
- CCCD: `00002902-0000-1000-8000-00805F9B34FB`

## Wi-Fi Direct Transport

- **High-bandwidth transport** for bulk transfers (maps, images, voice)
- On-demand connection establishment
- TCP-based data transfer over Wi-Fi P2P group
- DNS-SD service discovery for peer identification

## LoRa Transport (External Hardware)

- **Long-range transport** via external ESP32/SX1262 radio nodes
- Bluetooth Serial (SPP) or BLE GATT interface to phone
- AT command protocol over Bluetooth Serial
- Configurable spreading factor (SF7-SF12), bandwidth, coding rate

### Hardware Protocol (Bluetooth Serial)
```
AT+SEND=<hex_payload>          → Send Beacon frame
AT+RECV                        ← Receive Beacon frame (async)
AT+CONFIG=<key>=<value>        → Configure radio
AT+STATUS                      ← Status JSON
```

## RadioManager

Central coordinator that manages all transports:

```kotlin
val radioManager = RadioManager(context, RadioConfig(
    enableBle = true,
    enableWifiDirect = true,
    enableLora = false
), meshEngine)

radioManager.start()

// Send to all available transports
radioManager.sendToAll(frame)
```

## Power Management Integration

Transports adapt to power modes automatically:

| Power Mode | BLE Scan Interval | BLE Advertise | Wi-Fi Direct | LoRa Duty Cycle |
|------------|-------------------|---------------|--------------|-----------------|
| NORMAL | 2000ms/200ms | 500ms | Active | 10% |
| CONSERVATION | 5000ms/250ms | 1000ms | On-demand | 5% |
| SURVIVAL | 30000ms/500ms | 2000ms | Disabled | 1% |
| CRITICAL | 60000ms/1000ms | 5000ms | Disabled | 0.1% |

## Building

```bash
./gradlew :beacon-radio:kotlin:assemble
```

## Testing

```bash
# Unit tests
./gradlew :beacon-radio:kotlin:test

# Integration tests (requires 2 devices)
./gradlew :beacon-radio:kotlin:connectedAndroidTest
```

## License

MIT License — see [LICENSE](../../LICENSE)