# Architecture Decision Records (ADR) Index

## Document Metadata

* **Document ID:** `ADR-INDEX`
* **Version:** `1.0.0`
* **Status:** Active
* **Author:** Project Beacon Core Team
* **Last Updated:** 2026-08-20

---

## Purpose

This index tracks all Architecture Decision Records for Project Beacon. Each ADR documents a significant architectural decision, its context, alternatives considered, and consequences.

---

## ADR Lifecycle

| Status | Description |
|--------|-------------|
| **Draft** | Under development, not yet reviewed |
| **Proposed** | Submitted for review |
| **Accepted** | Approved and implemented |
| **Superseded** | Replaced by newer ADR |
| **Rejected** | Not adopted |

---

## ADR Registry

| ID | Title | Status | Date | Supersedes |
|----|-------|--------|------|------------|
| [ADR-0000](ADR-0000-template.md) | ADR Template | Accepted | 2026-08-20 | — |
| [ADR-0001](ADR-0001-ble-discovery.md) | Primary Local Discovery Mechanism (BLE) | Accepted | 2026-08-20 | — |
| [ADR-0002](ADR-0002-gis-database.md) | Offline GIS Database Engine Selection (SpatiaLite + DuckDB) | Accepted | 2026-08-20 | — |
| ADR-0003 | Mesh Routing Algorithm Selection | Proposed | — | — |
| ADR-0004 | Transport Abstraction Layer Design | Draft | — | — |
| ADR-0005 | Identity & Key Management Architecture | Draft | — | — |
| ADR-0006 | Power Management State Machine | Draft | — | — |
| ADR-0007 | Store-and-Forward Bundle Protocol | Draft | — | — |
| ADR-0008 | External Radio Hardware Interface | Draft | — | — |
| ADR-0009 | Local AI Model Selection & Runtime | Draft | — | — |
| ADR-0010 | Map Data Format & Distribution | Draft | — | — |

---

## Proposed ADRs (Backlog)

### ADR-0003: Mesh Routing Algorithm Selection
**Context**: Need to choose primary routing algorithm for multi-hop mesh.
**Options**:
- Hybrid (Geographic + Epidemic) — Current prototype
- Reticulum (NRF) — Existing protocol, proven
- Meshtastic — Existing LoRa mesh, large community
- Custom DTN (Bundle Protocol) — Standards-based
- BATMAN-adv — Kernel-level, WiFi-focused

**Decision Criteria**: Performance at scale, battery efficiency, implementation complexity, interoperability.

---

### ADR-0004: Transport Abstraction Layer Design
**Context**: Define interface between mesh layer and radio transports.
**Key Questions**:
- Synchronous vs asynchronous API?
- Backpressure handling?
- Transport capabilities negotiation?
- Fragmentation at transport or mesh layer?

---

### ADR-0005: Identity & Key Management Architecture
**Context**: Define how identities are created, stored, rotated, revoked.
**Key Questions**:
- Single identity per device or per app install?
- Cross-device identity sync?
- Hardware-backed vs software keys?
- Revocation distribution mechanism?

---

### ADR-0006: Power Management State Machine
**Context**: Formalize power modes and transitions.
**Key Questions**:
- Exact battery thresholds?
- Hysteresis to prevent oscillation?
- User override behavior?
- Background execution limits per Android version?

---

### ADR-0007: Store-and-Forward Bundle Protocol
**Context**: Select DTN bundle protocol for intermittent connectivity.
**Options**:
- RFC 5050 (BPv6) — Standard, complex
- RFC 9171 (BPv7) — Modern, simpler
- Custom minimal bundle — Tailored, no interop
- Reticulum bundles — Proven, integrated

---

### ADR-0008: External Radio Hardware Interface
**Context**: Define phone ↔ radio node communication protocol.
**Key Questions**:
- Bluetooth Serial (SPP) vs BLE GATT vs USB?
- AT command set vs binary protocol?
- Firmware update mechanism?
- Radio config persistence?

---

### ADR-0009: Local AI Model Selection & Runtime
**Context**: Choose offline LLM for summarization, translation, triage.
**Options**:
- Llama.cpp (GGUF) — Mature, many models
- ExecuTorch — PyTorch mobile, optimized
- ONNX Runtime — Cross-platform, hardware accel
- MLC LLM — Compiler-based, fast
- Custom (llama.rs) — Rust native

**Constraints**: < 2GB RAM, < 4GB storage, runs on mid-range Android.

---

### ADR-0010: Map Data Format & Distribution
**Context**: Decide on vector tile format, update mechanism, coverage.
**Key Questions**:
- MBTiles vs GeoPackage vs PMTiles?
- Planet-scale vs regional extracts?
- Mesh sync vs manual update?
- Attribution compliance (OSM)?

---

## ADR Template

See [ADR-0000-template.md](ADR-0000-template.md) for the standard format.

### Required Sections

1. **Status** — Draft/Proposed/Accepted/Rejected/Superseded
2. **Context** — Problem, constraints, forces
3. **Decision** — Chosen solution with rationale
4. **Alternatives Considered** — Options evaluated and why rejected
5. **Consequences** — Positive, negative, risks, dependencies
6. **References** — Links to related docs, issues, PRs

---

## Creating a New ADR

1. Copy `ADR-0000-template.md` to `ADR-XXXX-title.md`
2. Fill in all sections
3. Submit PR with `adr/` label
4. Request review from maintainers
5. Upon acceptance, update this index

---

## ADR Guidelines

- **One decision per ADR** — Keep focused
- **Immutable once Accepted** — Don't edit; supersede instead
- **Include trade-offs** — Honest assessment of negatives
- **Link to implementation** — PR, commit, or issue
- **Review periodically** — Annual review for active ADRs

---

## References

* [Michael Nygard's ADR Format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
* [ADR GitHub Organization](https://github.com/adr)
* [Project Beacon Architecture](../architecture/architecture.md)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 1.0.0 | Initial index with ADR-0000, ADR-0001, ADR-0002 | Project Beacon Core Team |