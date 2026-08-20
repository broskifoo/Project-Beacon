# UI/UX Specification

## Document Metadata

* **Document ID:** `DOC-UX-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the user experience and interface design for Project Beacon applications, covering the Android mobile app, web dashboard, and CLI tools.

---

## Design Principles

| Principle | Description |
|-----------|-------------|
| **Offline-First** | All core features work without Internet; online is enhancement |
| **Glanceable** | Critical info visible in < 2 seconds; minimal taps for common actions |
| **Power-Aware** | UI adapts to power mode; dark mode default; minimal animations |
| **Accessible** | WCAG 2.1 AA; TalkBack/VoiceOver; high contrast; scalable text |
| **Resilient** | Graceful degradation; clear error states; offline indicators |
| **Trustworthy** | Clear signal of message security; verification workflows |

---

## User Flows

### Flow 1: First Launch & Onboarding

```
┌─────────────┐
│  Welcome    │ ──► Permissions (Location, Bluetooth, Notifications)
└─────────────┘
       │
       ▼
┌─────────────┐
│  Identity   │ ──► Generate Ed25519 keypair in Secure Element
│  Setup      │     Display fingerprint for verification
└─────────────┘
       │
       ▼
┌─────────────┐
│  Power Mode │ ──► Choose default (Normal/Conservation)
│  Preference │     Explain battery impact
└─────────────┘
       │
       ▼
┌─────────────┐
│  Map Data   │ ──► Download region (Wi-Fi recommended)
│  (Optional) │     Show storage estimate
└─────────────┘
       │
       ▼
┌─────────────┐
│  Ready      │ ──► Main screen
└─────────────┘
```

### Flow 2: Emergency SOS Activation

```
┌─────────────────────────────────────────────────────────────┐
│                    MAIN SCREEN                              │
│  [📍 Map] [💬 Messages] [🚨 SOS] [📊 Network] [⚙️ Settings] │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Long press SOS 2 seconds)
┌─────────────────────────────────────────────────────────────┐
│                    SOS CONFIRMATION                         │
│                                                              │
│     ╔═══════════════════════════════════════════════════╗    │
│     ║  ⚠️  SEND EMERGENCY ALERT?                         ║    │
│     ║                                                    ║    │
│     ║  This will broadcast your location to all nearby   ║    │
│     ║  Beacon nodes and rescue teams.                    ║    │
│     ║                                                    ║    │
│     ║  Includes: GPS location, battery level, your ID    ║    │
│     ╚═══════════════════════════════════════════════════╝    │
│                                                              │
│     [Cancel]                                    [SEND SOS]  │
│        (auto-cancels in 10s if no action)                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    SOS SENT                                 │
│                                                              │
│     ✓ SOS transmitted (CRITICAL priority)                   │
│     ✓ Location: 40.7128° N, 74.0060° W                      │
│     ✓ Battery: 67%                                          │
│     ✓ Retry: Every 30s until acknowledged                   │
│                                                              │
│     [Add Details]  [Cancel Retry]  [Close]                  │
└─────────────────────────────────────────────────────────────┘
```

### Flow 3: Message Composition

```
┌─────────────────────────────────────────────────────────────┐
│                    MESSAGE THREAD                           │
│  ◀ Back                              [📎] [📍] [🎤] [⋮]     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ◆ Sarah (2 min ago)                                        │
│     "On my way to shelter"                                   │
│     ✓ Delivered 2 min ago                                    │
│                                                              │
│  ● You (1 min ago)                                          │
│     "Copy, at checkpoint 3"                                  │
│     ⏳ Sending...                                            │
│                                                              │
│  ◆ Sarah (30 sec ago)                                       │
│     "Checkpoint 3 clear"                                     │
│     ✓ Delivered                                              │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  [Type message...                          ][Send]          │
└─────────────────────────────────────────────────────────────┘
```

---

## Screen Specifications

### Screen 1: Main Map View (Hub)

```
┌─────────────────────────────────────────────────────────────┐
│  BEACON                                    [🔋 87%] [⋮]     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                  MAP VIEWPORT                        │   │
│   │                                                      │   │
│   │   ● You (GPS: 12m accuracy)                         │   │
│   │   ◆ Node-A8F2  -62dBm  94%  2m ago                  │   │
│   │   ◆ Radio-Node3 -71dBm  67%  5m ago                 │   │
│   │   💧 Water Source (verified 3/3)  1.2km             │   │
│   │   🏥 Medical Clinic (verified 2/3)  2.8km           │   │
│   │   🏠 Community Shelter  850m                         │   │
│   │   ⚠️ Flooded Road (reported 15m ago)                │   │
│   │                                                      │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [📍 Center]  [💬 Messages]  [🚨 SOS]  [📊 Network]  [⚙️]    │
└─────────────────────────────────────────────────────────────┘
```

**Map Interactions:**
- Tap marker → bottom sheet with details
- Long press → add custom marker
- Two-finger rotate/tilt
- Double-tap zoom in; two-finger tap zoom out
- Compass button → reorient to North

### Screen 2: Messages List

```
┌─────────────────────────────────────────────────────────────┐
│  MESSAGES                                    [⋮] [✎ New]    │
├─────────────────────────────────────────────────────────────┤
│  🔴 CRITICAL (2)                                            │
│  ├── SOS: John Doe — 3 min ago — ✓ Acknowledged             │
│  └── Medical Emergency: Jane Smith — 12 min ago — ⏳ Sending │
│                                                             │
│  🟠 HIGH (1)                                                │
│  └── Resource Request: Water at Main St — 5 min ago — ✓     │
│                                                             │
│  🟡 NORMAL (5)                                              │
│  ├── Team Alpha: "At checkpoint 3" — 2 min ago — ✓          │
│  ├── Sarah: "On my way" — 15 min ago — ✓                    │
│  └── ...                                                    │
│                                                             │
│  🟢 LOW (3)                                                 │
│  └── Weather Update: Storm passing — 1 hour ago — ✓         │
└─────────────────────────────────────────────────────────────┘
```

**Priority Colors:** CRITICAL=Red, HIGH=Orange, NORMAL=Amber, LOW=Green

### Screen 3: Network Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  NETWORK                                     [⋮] [🔄 Rescan] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              TOPOLOGY VISUALIZATION                  │   │
│   │                                                      │   │
│   │      ● You ── ◆ Node-A ── ◆ Node-B                  │   │
│   │           ╲                                    ╱    │   │
│   │            ╲  ⚡ Radio-Node3  (LoRa)  ╱            │   │
│   │             ╲                            ╱         │   │
│   │              ◆ Node-C ◄─── ◆ Node-D                  │   │
│   │                                                      │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   PEERS (12 nearby)                                         │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ ◆ Phone-A8F2   BLE  -62dBm  94%  NORMAL  2m ago     │   │
│   │ ◆ Radio-Node3  LoRa -71dBm  67%  CONSERV  5m ago     │   │
│   │ ◆ Phone-4K9L   BLE  -78dBm  23%  SURVIVAL 1m ago     │   │
│   │ ✓ Trusted    ✗ Blocked    ⚡ Radio                   │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   STATS                                                     │
│   Messages: 1,234 sent • 1,198 received • 97% delivered    │
│   Uptime: 4h 23m • Avg latency: 2.3s • 3 hops max          │
└─────────────────────────────────────────────────────────────┘
```

### Screen 4: Resource Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  RESOURCES                                    [⋮] [+ Report] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   💧 WATER (3)                                              │
│   ├── Community Center Well — 1.2km — ✓✓✓ Verified        │
│   │   "Working hand pump, good taste" — 10 min ago         │
│   ├── Fire Station Hydrant — 2.1km — ✓✓ Verified          │
│   └── Park Fountain — 3.5km — ⚠ Unverified               │
│                                                             │
│   🏥 MEDICAL (2)                                            │
│   ├── Downtown Clinic — 2.8km — ✓✓✓ Operational           │
│   │   "Open 24h, trauma capable" — 30 min ago             │
│   └── Mobile Medic Unit — 4.2km — ✓✓ En route             │
│                                                             │
│   🏠 SHELTER (1)                                            │
│   └── High School Gym — 850m — ✓✓✓ Open                   │
│       "Capacity 200, pets allowed" — 1 hour ago           │
│                                                             │
│   ⚠️ HAZARDS (2)                                            │
│   ├── Flooded: Main St Bridge — 1.5km — ✓✓ Confirmed      │
│   └── Road Closed: Highway 101 — 5km — ✓ Official         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Confidence Indicators:** ✓✓✓ = High (3+ independent), ✓✓ = Medium (2), ✓ = Low (1), ⚠ = Unverified

### Screen 5: Settings

```
┌─────────────────────────────────────────────────────────────┐
│  SETTINGS                                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ▸ NETWORK                                                  │
│     Transports: [✓] BLE  [✓] Wi-Fi Direct  [ ] LoRa        │
│     Max Peers: 50          TTL: 5 hops                      │
│     Auto-accept peers: [ ]                                  │
│                                                             │
│  ▸ POWER                                                    │
│     Default Mode: [NORMAL ▼]                                │
│     Auto-switch: [✓] Below 50% → Conservation              │
│                   [✓] Below 20% → Survival                  │
│                   [✓] Below 10% → Critical                  │
│     Background scan: [Aggressive ▼]                         │
│                                                             │
│  ▸ SECURITY                                                 │
│     Identity: [View Fingerprint] [Rotate Key]               │
│     Trusted Peers: 8    Blocked: 2                          │
│     Encrypt storage: [✓] (requires passphrase)              │
│                                                             │
│  ▸ MAPS                                                     │
│     Downloaded: North America (2.3 GB) [Manage]            │
│     Auto-update: [✓] Wi-Fi only                             │
│     Rendering: [Vector ▼]                                   │
│                                                             │
│  ▸ NOTIFICATIONS                                            │
│     SOS Alerts: [✓] High Priority: [✓] Normal: [ ]         │
│     Vibration: [✓]    Sound: [Critical only ▼]             │
│                                                             │
│  ▸ ABOUT                                                    │
│     Version: 0.1.0-alpha    Build: 20260820                │
│     [View Licenses] [Report Bug] [Privacy Policy]          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Visual Design System

### Color Palette

| Role | Light Mode | Dark Mode | Usage |
|------|------------|-----------|-------|
| **Primary** | #006D6D | #00B5B5 | Brand, primary actions |
| **Primary Container** | #A7F0F0 | #004D4D | Chips, selected states |
| **Secondary** | #526D52 | #B5D5B5 | Secondary actions |
| **Surface** | #FFFFFF | #121212 | Backgrounds, cards |
| **Surface Variant** | #F0F0F0 | #1E1E1E | Elevated surfaces |
| **Outline** | #707070 | #8A8A8A | Dividers, borders |
| **Error** | #BA1A1A | #FFB4AB | Critical, destructive |
| **Warning** | #8C6100 | #FFD67A | High priority, caution |
| **Success** | #1A6D1A | #8AFF8A | Delivered, verified |
| **Info** | #0059A0 | #A0D0FF | Normal, info |

### Priority Colors (Semantic)

| Priority | Color (Dark) | Color (Light) | Icon |
|----------|--------------|---------------|------|
| CRITICAL | #FF4444 | #CC0000 | 🔴 / ⚠️ |
| HIGH | #FF8800 | #CC6600 | 🟠 / ⬆️ |
| NORMAL | #FFCC00 | #CC9900 | 🟡 / 📝 |
| LOW | #44AA44 | #008800 | 🟢 / 📄 |

### Typography

| Style | Font | Size | Weight | Line Height |
|-------|------|------|--------|-------------|
| **Display Large** | Roboto | 57sp | Regular | 64sp |
| **Display Medium** | Roboto | 45sp | Regular | 52sp |
| **Headline Large** | Roboto | 32sp | Regular | 40sp |
| **Headline Medium** | Roboto | 28sp | Regular | 36sp |
| **Title Large** | Roboto | 22sp | Medium | 28sp |
| **Title Medium** | Roboto | 16sp | Medium | 24sp |
| **Body Large** | Roboto | 16sp | Regular | 24sp |
| **Body Medium** | Roboto | 14sp | Regular | 20sp |
| **Label Large** | Roboto | 14sp | Medium | 20sp |
| **Body Small** | Roboto | 12sp | Regular | 16sp |

### Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| space-1 | 4dp | Icon gaps, inline |
| space-2 | 8dp | Component padding |
| space-3 | 12dp | Related elements |
| space-4 | 16dp | Screen margins, card padding |
| space-5 | 24dp | Section gaps |
| space-6 | 32dp | Major sections |
| space-7 | 48dp | Page-level |

### Iconography

- **System**: Material Icons (outlined style)
- **Custom**: Beacon-specific (SOS, mesh, radio, etc.)
- **Size**: 24dp default; 16dp inline; 32dp emphasis
- **Touch target**: 48×48dp minimum

---

## Dark Mode

- **Default**: Dark mode (power saving on OLED)
- **Switch**: Auto (system) / Manual toggle in settings
- **Maps**: Dark map style (MapLibre dark theme)
- **Images**: Auto-invert prevented for photos/map tiles

---

## Accessibility

| Requirement | Implementation |
|-------------|----------------|
| **Screen Readers** | All elements labeled; live regions for status |
| **Touch Targets** | 48×48dp minimum; 8dp spacing |
| **Contrast** | 4.5:1 text; 3:1 UI elements |
| **Text Scaling** | Supports 200% system font size |
| **Color Blind** | Not color-only; icons + text |
| **Motion** | Respects "Reduce Motion" setting |
| **Language** | RTL layouts; font fallbacks |

---

## Error & Empty States

### Error States

```
┌─────────────────────────────────────────────────────────────┐
│  ⚠️  UNABLE TO SEND                                         │
│                                                              │
│  No nearby peers found. Message queued for retry.          │
│                                                              │
│  [Retry Now]  [View Network]  [Dismiss]                     │
└─────────────────────────────────────────────────────────────┘
```

### Empty States

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│        📭  NO MESSAGES YET                                   │
│                                                              │
│  Messages from nearby peers will appear here.               │
│                                                              │
│  [Compose Message]  [Scan for Peers]                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Web Dashboard (Desktop)

### Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  BEACON DASHBOARD    [Node Selector ▼]    [Settings] [Profile]  │
├──────────────┬──────────────────────────────────────────────────┤
│              │                                                  │
│  NAVIGATION  │              MAIN CONTENT                        │
│              │                                                  │
│  📍 Map      │  ┌──────────────────────────────────────────┐   │
│  💬 Messages │  │                                          │   │
│  📊 Network  │  │        INTERACTIVE MAP                   │   │
│  🏥 Resources│  │        (MapLibre GL, full screen)        │   │
│  ⚠️ Alerts   │  │                                          │   │
│  📈 Analytics│  └──────────────────────────────────────────┘   │
│  ⚙️ Config   │                                                  │
│              │  SIDE PANEL (collapsible)                        │
│              │  ┌──────────────────────────────────────────┐   │
│              │  │ Peer List | Message Log | Resource Table │   │
│              │  └──────────────────────────────────────────┘   │
│              │                                                  │
└──────────────┴──────────────────────────────────────────────────┘
```

### Features

- Real-time mesh topology visualization (Cytoscape.js)
- Message log with filtering (priority, peer, time)
- Resource management (CRUD, verification)
- Alert broadcasting with geographic targeting
- Historical analytics (delivery rates, battery trends)
- Multi-node fleet management

---

## CLI Tool (`beacon-cli`)

```
$ beacon --help
Usage: beacon <command> [options]

Commands:
  send       Send a message to peer or broadcast
  receive    Listen for incoming messages
  peers      List discovered peers
  network    Show mesh topology
  sos        Send emergency SOS
  identity   Manage identity keys
  config     View/modify configuration
  version    Show version info

Options:
  --node <id>       Target specific peer
  --priority <p>    CRITICAL|HIGH|NORMAL|LOW
  --ttl <n>         Time-to-live in hops
  --format <f>      json|table|csv
  --verbose         Debug output

Examples:
  beacon send --broadcast --priority HIGH "Team: regroup at checkpoint 3"
  beacon send --node ABCD1234 --priority CRITICAL "MEDICAL EMERGENCY"
  beacon peers --format json
  beacon network --topology
```

---

## Responsive Breakpoints (Android)

| Breakpoint | Width | Layout |
|------------|-------|--------|
| **Compact** | < 600dp | Single pane; bottom nav; modal sheets |
| **Medium** | 600-840dp | Two-pane (list + detail); rail nav |
| **Expanded** | > 840dp | Three-pane; permanent nav rail |

---

## Internationalization

| Language | Status | Notes |
|----------|--------|-------|
| English | Complete | Source language |
| Spanish | Planned | High priority (disaster zones) |
| French | Planned | High priority |
| Portuguese | Planned | High priority |
| Arabic | Planned | RTL support needed |
| Chinese (Simplified) | Planned | Font fallback |
| Hindi | Planned | Font fallback |
| Swahili | Planned | East Africa focus |

---

## Usability Testing Plan

| Test | Method | Participants | Metrics |
|------|--------|--------------|---------|
| **SOS Activation** | Task-based | 20 (mixed) | Time, errors, confidence |
| **Message Composition** | Task-based | 20 | Completion rate, time |
| **Map Navigation** | Think-aloud | 15 | Success rate, disorientation |
| **Peer Discovery** | Field test | 10 pairs | Discovery time, reliability |
| **Power Mode Switching** | Scenario | 15 | Understanding, trust |
| **Accessibility** | Audit + user test | 8 (disabled) | WCAG compliance |

---

## References

* [PRD](../prd/prd.md)
* [Architecture](../architecture/architecture.md)
* [Material Design 3](https://m3.material.io/)
* [WCAG 2.1](https://www.w3.org/WAI/WCAG21/quickref/)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |