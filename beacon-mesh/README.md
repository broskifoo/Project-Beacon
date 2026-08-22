# beacon-mesh

**Project Beacon Mesh Engine** — Delay-tolerant mesh routing and store-and-forward networking.

## Overview

`beacon-mesh` implements the mesh networking layer for Project Beacon, providing:

- **DTN Bundle Protocol**: Store-carry-forward messaging for intermittent connectivity
- **Hybrid Routing**: Geographic (GPSR-like) + Epidemic (probabilistic flooding)
- **Custody Transfer**: Reliable hop-by-hop delivery with retransmission
- **Neighbor Management**: Link quality tracking, peer discovery integration
- **Topology Dissemination**: Network state sharing for routing decisions

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON MESH                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Bundle     │  │   Routing    │  │  Neighbor    │          │
│  │  Manager     │  │   Engine     │  │   Table      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                    │
│         └─────────────────┼─────────────────┘                    │
│                           ▼                                      │
│              ┌────────────────────────┐                          │
│              │     MESH ENGINE        │                          │
│              │  • Receive & Forward   │                          │
│              │  • Route Computation   │                          │
│              │  • Custody Management  │                          │
│              │  • Topology Sync       │                          │
│              └────────────────────────┘                          │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                    │
│         ▼                 ▼                 ▼                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │   BLE       │  │  Wi-Fi      │  │   LoRa      │                │
│  │  Transport  │  │  Transport  │  │  Transport  │                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

| Component | Description |
|-----------|-------------|
| `MeshEngine` | Main coordinator, receives bundles, makes forwarding decisions |
| `Bundle` | DTN bundle with custody transfer support |
| `RoutingEngine` | Hybrid routing (geographic + epidemic) |
| `NeighborTable` | Peer tracking with link quality |
| `CustodyManager` | Hop-by-hop reliability with retransmission |
| `TopologyManager` | Network state dissemination |
| `TaskScheduler` | Background task coordination |

## Routing Algorithms

### Hybrid Routing (Default)

```
┌─────────────────────────────────────────────────────────────┐
│                    FORWARDING DECISION                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. DUPLICATE CHECK → Drop if seen                          │
│  2. TTL CHECK → Drop if hop_count >= max_hops               │
│  3. LOCAL DELIVERY → If destination == local                │
│  4. PRIORITY BOOST → CRITICAL always forwards               │
│  5. GEOGRAPHIC → If destination known & have location       │
│     → Forward to neighbor closer to destination             │
│  6. EPIDEMIC → Probabilistic flooding to subset of neighbors│
│     • CRITICAL: 100%                                        │
│     • HIGH: 75%                                             │
│     • NORMAL: 50%                                           │
│     • LOW: 5%                                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Geographic Routing (GPSR-inspired)

- Requires location awareness (GPS)
- Greedy forwarding: choose neighbor closest to destination
- Perimeter mode when stuck (not yet implemented)

### Epidemic Routing (Probabilistic Flooding)

- Flood with probability P
- Duplicate detection via bloom filter
- Priority-weighted probability

## Custody Transfer (DTN)

```
Node A                    Node B                    Node C
  │                         │                         │
  ├─ Bundle (custody) ────►│                         │
  │                         ├─ ACK (custody accepted) │
  │◄───────────────────────┤                         │
  │                         │                         │
  │                    (Node B now responsible)      │
  │                         │                         │
  │                    ┌────┴────┐                    │
  │                    ▼         ▼                    │
  │              Delivered   Forward                  │
  │                         │                         │
  │                         ▼                         │
  │                    (Node C custody)               │
  │                         │                         │
```

## Building

```bash
cd rust/mesh-engine
cargo build --release

# Run tests
cargo test

# Benchmarks
cargo bench
```

## Configuration

```rust
let config = MeshConfig {
    max_hops: 5,
    default_ttl_seconds: 3600,
    bundle_buffer_size: 1000,
    neighbor_timeout_seconds: 300,
    topology_broadcast_interval_seconds: 30,
    enable_geographic_routing: true,
    enable_epidemic_routing: true,
    forwarding_probability: 0.5,
};

let engine = MeshEngine::new(config);
engine.start().await?;
```

## Integration

```rust
// Receive bundle from radio transport
let result = engine.receive_bundle(bundle, &from_peer, link_quality).await?;

match result {
    ReceiveResult::Delivered => { /* notify application */ }
    ReceiveResult::Forward { bundle, next_hops } => {
        for next_hop in next_hops {
            radio.send(next_hop, bundle.clone()).await?;
        }
    }
    ReceiveResult::Duplicate => { /* ignore */ }
    _ => { /* handle */ }
}

// Update neighbor info from radio
engine.update_neighbor(neighbor_info).await?;
```

## Testing

```bash
# Unit tests
cargo test

# Property-based tests
cargo test --proptest

# Integration with simulator
cd ../../beacon-simulator/python
python -m pytest tests/test_mesh_integration.py
```

## Performance Targets

| Metric | Target |
|--------|--------|
| Bundle receive → forward decision | < 1 ms |
| Routing table lookup | < 10 µs |
| Neighbor table update | < 50 µs |
| Memory usage (1000 bundles) | < 10 MB |
| CPU (idle, 100 peers) | < 1% |

## License

MIT License — see [LICENSE](../../LICENSE)