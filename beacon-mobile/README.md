# beacon-mobile

**Project Beacon Mobile App** — Android application for disaster-resilient mesh communications.

## Overview

The main user-facing Android application that provides offline messaging, SOS, mesh networking, maps, and resource sharing capabilities.

## Features

- **Offline Messaging** — Text, location, SOS without Internet
- **Emergency SOS** — One-tap distress signal with GPS location
- **Mesh Networking** — Multi-hop BLE/Wi-Fi/LoRa communication
- **Offline Maps** — Vector tiles, POI search, routing
- **Resource Sharing** — Community reports (water, medical, shelter)
- **Alert Broadcasting** — Emergency notifications with geo-targeting
- **Power Management** — Battery-aware modes (Normal → Conservation → Survival → Critical)

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON MOBILE                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    UI LAYER (Compose)                    │    │
│  │  • Home, Map, Messages, Network, Resources, Alerts,      │    │
│  │    Settings screens                                      │    │
│  │  • ViewModels, StateFlow, Navigation                     │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    BEACON SDK                             │    │
│  │  • MessagingApi, PeerApi, NetworkApi                     │    │
│  │  • MapsApi, IdentityApi, PowerApi, StorageApi            │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                           │
│         ┌───────────┼───────────┐                               │
│         ▼           ▼           ▼                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │  beacon-core│  │ beacon-mesh │  │ beacon-radio │                │
│  │  (storage,  │  │  (routing,  │  │  (BLE,       │                │
│  │   bundle)   │  │   custody)  │  │   WiFi, LoRa)│                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Requirements

- Android 7.0+ (API 24)
- Bluetooth LE hardware
- Location permission (for BLE scanning)
- Camera permission (for QR code scanning - optional)

## Building

```bash
# From project root
./gradlew :beacon-mobile:app:assembleDebug

# Or open in Android Studio
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `BLUETOOTH_CONNECT` | Connect to BLE peers |
| `BLUETOOTH_SCAN` | Discover BLE peers |
| `BLUETOOTH_ADVERTISE` | Advertise presence |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning |
| `ACCESS_BACKGROUND_LOCATION` | Background mesh participation |
| `NEARBY_WIFI_DEVICES` | Wi-Fi Direct discovery |
| `FOREGROUND_SERVICE` | Background mesh service |
| `WAKE_LOCK` | Prevent CPU sleep during mesh ops |
| `POST_NOTIFICATIONS` | SOS, message alerts |

## Project Structure

```
beacon-mobile/
├── app/
│   ├── src/main/
│   │   ├── java/org/beacon/mobile/
│   │   │   ├── BeaconApplication.kt       # Application class
│   │   │   ├── AppLifecycleObserver.kt    # Lifecycle observer
│   │   │   ├── service/
│   │   │   │   └── MeshForegroundService.kt # Background mesh service
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt        # Main entry point
│   │   │   │   ├── navigation/            # Compose navigation
│   │   │   │   ├── screens/               # Screen composables
│   │   │   │   ├── components/            # Reusable UI components
│   │   │   │   └── theme/                 # Material3 theme
│   │   │   └── viewmodel/
│   │   │       └── MainViewModel.kt       # Main state holder
│   │   ├── res/
│   │   │   ├── values/                    # Strings, colors, themes
│   │   │   ├── drawable/                  # Drawable resources
│   │   │   ├── xml/                       # XML configs
│   │   │   └── layout/                    # Legacy layouts (minimal)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Components

### BeaconApplication
- Initializes Beacon SDK with configuration
- Manages preferences via DataStore
- Provides WorkManager configuration

### MeshForegroundService
- Runs mesh networking in background
- Manages wake locks
- Handles notifications (SOS, messages)
- Adjusts behavior based on power mode

### MainViewModel
- Central state holder for UI
- Observes SDK streams (peers, messages, power, battery)
- Coordinates navigation

### Screens
- **HomeScreen** — Dashboard with quick actions
- **MapScreen** — Offline map with peers/resources
- **MessagesScreen** — Message threads & composer
- **NetworkScreen** — Peer list, signal quality, topology
- **ResourcesScreen** — Community resource reports
- **AlertsScreen** — Emergency broadcasts
- **SettingsScreen** — Network, power, security, maps, notifications
- **SosScreen** — Emergency SOS confirmation

## Development

### Running
```bash
# Debug build
./gradlew :beacon-mobile:app:installDebug

# Release build
./gradlew :beacon-mobile:app:assembleRelease
```

### Testing
```bash
# Unit tests
./gradlew :beacon-mobile:app:test

# Instrumented tests (requires device/emulator)
./gradlew :beacon-mobile:app:connectedAndroidTest
```

### Lint
```bash
./gradlew :beacon-mobile:app:lint
```

## Configuration

### BeaconConfig (in BeaconApplication)
```kotlin
BeaconConfig(
    deviceName = "Beacon Node",
    enableBle = true,
    enableWifiDirect = true,
    enableLora = false,
    powerMode = PowerMode.NORMAL,
    maxPeers = 50,
    messageTtl = 5
)
```

### Power Modes
| Mode | Battery | Scan Interval | Advertise Interval | Behavior |
|------|---------|---------------|-------------------|----------|
| NORMAL | > 50% | 2000ms/200ms | 500ms | Full mesh participation |
| CONSERVATION | 20-50% | 5000ms/250ms | 1000ms | Reduced scanning |
| SURVIVAL | 10-20% | 30000ms/500ms | 2000ms | Minimal scanning |
| CRITICAL | < 10% | 60000ms/1000ms | 5000ms | Identity beacon only |

## License

MIT License — see [LICENSE](../../LICENSE)