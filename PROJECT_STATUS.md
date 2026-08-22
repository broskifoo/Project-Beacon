# Project Status Dashboard

This document provides a real-time, high-level overview of the current developmental state of Project Beacon.

---

## Project Metadata

| Attribute | Value / Status |
|---|---|
| **Current Version** | `v0.1.0-alpha` |
| **Current Milestone** | **Milestone 1 — Foundation** |
| **Release Track** | Pre-alpha / Architectural Bootstrap |
| **Last Updated** | 2026-08-20 |

---

## Active Documents

The following documents are currently being drafted, reviewed, or actively maintained:

| Document ID | Title | Status | Primary Owner | Target Completion |
|---|---|---|---|---|
| `DOC-VISION-001` | [Vision Document](docs/vision/vision.md) | **Approved** | Project Beacon Core Team | 2026-08-20 |
| `DOC-PRD-001` | [Product Requirements Document (PRD)](docs/prd/prd.md) | **Approved** | Project Beacon Core Team | 2026-08-20 |
| `DOC-SRS-001` | [Software Requirements Specification (SRS)](docs/srs/srs.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-ARCH-001` | [Architecture Overview](docs/architecture/architecture.md) | **Approved** | Project Beacon Core Team | 2026-08-20 |
| `DOC-PROTOCOL-001` | [Protocol Specification](docs/protocol/protocol.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-SEC-001` | [Security Specification](docs/security/security.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-HW-001` | [Hardware Specification](docs/hardware/hardware.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-UX-001` | [UI/UX Specification](docs/ui-ux/ui-ux.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-SDK-001` | [SDK Specification](docs/sdk/sdk.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |
| `DOC-TEST-001` | [Testing Strategy](docs/testing/testing.md) | **Draft** | Project Beacon Core Team | 2026-08-27 |

---

## Active Research

Current research initiatives exploring technology limits, algorithms, or hardware designs:

| Research ID | Topic / Goal | Status | Lead Researcher |
|---|---|---|---|
| `RES-MESH-001` | Evaluation of LoRa mesh routing protocols (Reticulum, Meshtastic, Custom Hybrid) | **Active** | Project Beacon Core Team |
| `RES-MAPS-001` | Memory-efficient offline vector tile rendering on mobile (MapLibre + SpatiaLite) | **Active** | Project Beacon Core Team |
| `RES-HW-001` | RF power consumption optimization for solar-powered beacon units (ESP32-S3 + SX1262) | **Active** | Project Beacon Core Team |
| `RES-CRYPTO-001` | Post-quantum cryptography readiness for identity keys | **Planned** | — |
| `RES-AI-001` | Offline LLM model selection for edge inference (llama.cpp vs ExecuTorch vs MLC) | **Planned** | — |

---

## Architecture Decision Records (ADRs)

Track of major architectural design decisions. See the `/adr` directory for template and specific files.

### Completed ADRs (Approved)

| ADR | Title | Status | Date |
|---|---|---|---|
| **ADR-0000** | [ADR Template](adr/ADR-0000-template.md) | **Approved** | 2026-08-20 |
| **ADR-0001** | [Primary Local Discovery Mechanism (BLE)](adr/ADR-0001-ble-discovery.md) | **Approved** | 2026-08-20 |
| **ADR-0002** | [Offline GIS Database Engine Selection (SpatiaLite + DuckDB)](adr/ADR-0002-gis-database.md) | **Approved** | 2026-08-20 |

### Proposed ADRs (Backlog)

| ADR | Title | Status |
|---|---|---|
| **ADR-0003** | Mesh Routing Algorithm Selection (Hybrid vs Reticulum vs Meshtastic) | Proposed |
| **ADR-0004** | Transport Abstraction Layer Design | Draft |
| **ADR-0005** | Identity & Key Management Architecture | Draft |
| **ADR-0006** | Power Management State Machine | Draft |
| **ADR-0007** | Store-and-Forward Bundle Protocol (DTN) | Draft |
| **ADR-0008** | External Radio Hardware Interface (Bluetooth Serial) | Draft |
| **ADR-0009** | Local AI Model Selection & Runtime | Draft |
| **ADR-0010** | Map Data Format & Distribution (MBTiles vs PMTiles) | Draft |

---

## Component Status

| Component | Language | Status | Progress | Notes |
|---|---|---|---|---|
| **beacon-sdk** | Kotlin Multiplatform | **Initialized** | 60% | Core interfaces, Android impl, models |
| **beacon-core** | Kotlin + Rust | **Initialized** | 40% | Models, APIs, storage layer |
| **beacon-mesh** | Rust + Kotlin | **Initialized** | 50% | Routing engine, custody, neighbor mgmt |
| **beacon-radio** | Kotlin + Rust | **Initialized** | 30% | BLE transport, transport abstraction |
| **beacon-dashboard** | TypeScript/React | **Initialized** | 35% | Web UI, MapLibre, Zustand store |
| **beacon-simulator** | Python + Rust | **Initialized** | 55% | Models, propagation, mobility, routing, CLI |
| **beacon-ai** | Python + Rust | **Not Started** | 0% | Planned for Milestone 6 |
| **beacon-hardware** | KiCad | **Not Started** | 0% | Planned for Milestone 7 |
| **beacon-os** | Yocto/Buildroot | **Not Started** | 0% | Planned for Milestone 9 |

---

## Infrastructure Status

| System | Status | Details |
|---|---|---|
| **Documentation Pipeline** | **Operational** | MkDocs Material + GitHub Actions (lint, build, deploy) |
| **CI/CD** | **Configured** | GitHub Actions: lint-docs, build-docs, deploy-docs |
| **Markdown Linting** | **Active** | markdownlint-cli2 with custom config |
| **Link Verification** | **Active** | lychee link checker |
| **GitHub Pages** | **Ready** | Configured for auto-deploy on main branch |

---

## Next Objectives

Immediate technical and organizational objectives for the upcoming development cycles:

### Milestone 1 — Foundation (Current Sprint Focus)
- [x] Finalize baseline hardware requirement specifications.
- [x] Draft initial core API interfaces in `beacon-sdk`.
- [x] Approve `DOC-VISION-001` and `DOC-PRD-001`.
- [x] Set up simulation environments in `beacon-simulator`.
- [x] Establish the automated docs builds pipeline (`workflows/docs.yml`).
- [x] Approve `DOC-ARCH-001` Architecture Overview.
- [x] Resolve ADR-0001 (BLE Discovery) and ADR-0002 (GIS Database).
- [x] Initialize all 9 component project structures with READMEs.

### Milestone 1 — Remaining Tasks (Week 2-4)
- [ ] Complete SRS (`DOC-SRS-001`) with detailed technical specs.
- [ ] Complete Protocol Specification (`DOC-PROTOCOL-001`).
- [ ] Complete Security Specification (`DOC-SEC-001`).
- [ ] Finish beacon-sdk Android implementation (all APIs).
- [ ] Implement beacon-core storage layer (SQLCipher integration).
- [ ] Complete beacon-mesh routing engine integration tests.
- [ ] Finish beacon-radio BLE transport (connection management).
- [ ] Complete beacon-dashboard MapView with MapLibre integration.
- [ ] Run beacon-simulator with 100-node scenario validation.
- [ ] Validate documentation pipeline end-to-end (PR → deploy).

### Milestone 2 — Communication MVP (Target: 4-6 weeks)
- [ ] Two Android phones exchange messages via BLE (Internet OFF).
- [ ] Message persistence survives app kill/reboot.
- [ ] Delivery status updates correctly (QUEUED→SENT→DELIVERED→ACKED).
- [ ] SOS message sends with location, triggers notification on peer.
- [ ] Battery drain < 5%/hour idle (NORMAL mode).
- [ ] App runs 24h without crash/ANR in background.

---

## Metrics

| Metric | Current | Target (M1) | Target (M2) |
|---|---|---|---|
| **Documentation Coverage** | 60% | 100% | 100% |
| **ADRs Approved** | 2/10 | 2/10 | 5/10 |
| **SDK API Coverage** | 80% | 100% | 100% |
| **Unit Test Coverage** | 0% | 60% | 80% |
| **Simulator Scenarios** | 3/10 | 5/10 | 10/10 |
| **Dashboard Views** | 6/6 | 6/6 | 6/6 |

---

## Blockers & Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Android BLE background execution limits (API 31+) | High | High | Foreground service + proper permissions; test on API 24-34 |
| SQLCipher Android integration complexity | Medium | Medium | Use proven wrapper libraries; test early |
| Mesh routing algorithm selection (ADR-0003) | High | Medium | Prototype multiple in simulator before deciding |
| External radio hardware procurement delays | Medium | Low | Software-first; validate with simulator before hardware |
| Offline map data licensing (OSM) | Low | Low | Use ODbL-compliant tooling; attribution in app |

---

## Team & Resources

| Role | Status |
|---|---|
| **Core Architect** | Active |
| **Android Engineer** | Active |
| **Rust/Embedded Engineer** | Active |
| **Networking/Protocol Engineer** | Active |
| **Frontend/React Engineer** | Active |
| **DevOps/Infrastructure** | Active |
| **Hardware/RF Engineer** | Needed (Milestone 7) |
| **AI/ML Engineer** | Needed (Milestone 6) |
| **Technical Writer** | Active |

---

*Last updated: 2026-08-20 by Project Beacon Core Team*