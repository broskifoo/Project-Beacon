# beacon-dashboard

**Project Beacon Dashboard** — Web and desktop administration interface for Beacon mesh networks.

## Overview

`beacon-dashboard` provides a comprehensive web-based interface for monitoring and managing Beacon mesh networks. It can also be packaged as a desktop application using Tauri.

## Features

- **Interactive Map** — Real-time peer locations, resource markers, mesh topology (MapLibre GL)
- **Message Center** — Send/receive messages, SOS, priority queues, delivery status
- **Network Monitor** — Peer list, signal quality, battery, trust scores, mesh topology (Cytoscape.js)
- **Resource Management** — Community resource reporting (water, medical, shelter, hazards)
- **Alert Broadcasting** — Geographic emergency alerts with severity levels
- **Settings** — Network, power, security, maps, notifications configuration

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON DASHBOARD                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    WEB UI (React)                        │    │
│  │  • Map View (MapLibre GL)                                │    │
│  │  • Messages, Network, Resources, Alerts, Settings        │    │
│  │  • Zustand state management                              │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                           │
│                     ▼                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              BEACON SDK (TypeScript)                     │    │
│  │  • MessagingApi, PeerApi, NetworkApi                     │    │
│  │  • MapsApi, IdentityApi, PowerApi                        │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                           │
│                     ▼                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              BEACON CORE (via gRPC/WS)                   │    │
│  │  • Bundle management, routing, neighbor tracking         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Framework** | React 18 + TypeScript |
| **Build** | Vite 5 |
| **Routing** | React Router 6 |
| **State** | Zustand |
| **Maps** | MapLibre GL |
| **Topology** | Cytoscape.js |
| **Styling** | CSS Variables + Utility Classes |
| **Icons** | Lucide React |
| **Desktop** | Tauri (planned) |

## Project Structure

```
beacon-dashboard/
├── web/                 # Web application
│   ├── src/
│   │   ├── components/  # Reusable UI components
│   │   ├── views/       # Page views (Map, Messages, Network, etc.)
│   │   ├── stores/      # Zustand stores
│   │   ├── hooks/       # Custom React hooks
│   │   └── utils/       # Utility functions
│   ├── public/          # Static assets
│   └── package.json
├── desktop/             # Tauri desktop app (planned)
├── shared/              # Shared types and utilities
└── package.json         # Root workspace config
```

## Views

| View | Path | Description |
|------|------|-------------|
| **Map** | `/map` | Interactive map with peers, resources, topology |
| **Messages** | `/messages` | Message threads, composition, priority queue |
| **Network** | `/network` | Peer list, signal quality, mesh topology |
| **Resources** | `/resources` | Community resource reporting |
| **Alerts** | `/alerts` | Emergency alert broadcasting |
| **Settings** | `/settings` | Network, power, security, maps, notifications |

## Development

```bash
cd web
npm install
npm run dev          # Start dev server at http://localhost:3000
npm run build        # Production build
npm run lint         # ESLint
npm run test         # Vitest
```

## Backend Connection

The dashboard connects to a Beacon Core instance via gRPC/WebSocket:

```typescript
// Configured via environment or settings
const BEACON_CORE_ENDPOINT = 'http://localhost:8080'
const BEACON_WS_ENDPOINT = 'ws://localhost:8080/ws'
```

## Desktop App (Tauri)

```bash
cd desktop
npm install
npm run tauri dev    # Development
npm run tauri build  # Production build
```

## Deployment

### Web (Static Hosting)

```bash
npm run build
# Deploy dist/ to any static host (Netlify, Vercel, GitHub Pages, etc.)
```

### Docker

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY web/package*.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

## Configuration

| Setting | Environment Variable | Default |
|---------|---------------------|---------|
| API Endpoint | `VITE_BEACON_API` | `http://localhost:8080` |
| WS Endpoint | `VITE_BEACON_WS` | `ws://localhost:8080/ws` |
| Map Style | `VITE_MAP_STYLE` | `osm` |
| Theme | `VITE_DEFAULT_THEME` | `dark` |

## License

MIT License — see [LICENSE](../../LICENSE)