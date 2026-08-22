# Vision Document

## Document Metadata

* **Document ID:** `DOC-VISION-001`
* **Version:** `1.0.0`
* **Status:** Approved
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

### Purpose
This Vision Document establishes the long-term strategic direction for Project Beacon. It defines the problem space, the desired end state, and the guiding principles that will govern all architectural and product decisions.

### Scope
This document covers the entire Project Beacon platform across all milestones, from the initial Android communication MVP through the potential Beacon OS distribution. It is the source of truth for "why" we build what we build.

---

## The Problem

### Infrastructure Fragility
Modern society depends on centralized digital infrastructure that is surprisingly fragile:

- **Cellular networks** require powered towers, backhaul connectivity, and centralized switching
- **Internet routing** depends on BGP, DNS, and certificate authorities
- **Cloud services** concentrate functionality in data centers vulnerable to power, network, and physical disruption
- **Navigation services** require continuous connectivity to map tiles and routing engines
- **Communication platforms** depend on central servers for message relay, identity, and discovery

### Disaster Scenarios
When disasters occur, this infrastructure fails in predictable ways:

| Scenario | Infrastructure Impact |
|----------|----------------------|
| Earthquake | Cell towers damaged, power grid severed, fiber cut |
| Flood | Equipment submerged, power lost, roads impassable |
| Wildfire | Towers burned, power cut preemptively, smoke blocks line-of-sight |
| Cyclone/Hurricane | Widespread tower damage, extended power outages |
| Large-scale outage | Cascading failures, congestion collapse, coordinated attack |
| Remote operations | No infrastructure ever existed |

### Human Impact
The consequences are measured in lives:

- **Separated families** cannot locate each other
- **First responders** lack coordination channels
- **Communities** cannot share resource locations (water, shelter, medical)
- **Individuals** cannot signal distress or receive alerts
- **Information vacuums** filled by rumors and misinformation

---

## The Vision

### Core Statement
> **An open, decentralized, energy-aware, disaster-resilient computing and communication platform that remains useful when conventional infrastructure fails.**

### Desired End State (10-Year Horizon)

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROJECT BEACON ECOSYSTEM                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Mobile    │  │   Mobile    │  │   Mobile    │   ...       │
│  │   App       │  │   App       │  │   App       │             │
│  │  (Android)  │  │  (Android)  │  │  (Android)  │             │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘             │
│         │                │                │                     │
│         │    ┌───────────┴───────────┐   │                     │
│         │    │   MESH NETWORK        │   │                     │
│         │    │  (BLE / WiFi / LoRa)  │   │                     │
│         │    └───────────┬───────────┘   │                     │
│         │                │                │                     │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐             │
│  │  Beacon     │  │  Beacon     │  │  Beacon     │             │
│  │  Radio      │  │  Radio      │  │  Radio      │             │
│  │  Node       │  │  Node       │  │  Node       │             │
│  │ (ESP32/LoRa)│  │ (ESP32/LoRa)│  │ (ESP32/LoRa)│             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              BEACON CORE PLATFORM                        │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
│  │  │Messaging│ │ Maps/Nav│ │ Resources│ │  Local  │       │   │
│  │  │         │ │         │ │          │ │    AI   │       │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Key Capabilities at Maturity

1. **Communication**: Text, location, and telemetry exchange between devices without Internet
2. **Mesh Networking**: Multi-hop, store-and-forward routing across heterogeneous transports
3. **Offline Navigation**: Vector maps, routing, and resource markers stored entirely locally
4. **Community Dashboard**: Distributed situational awareness (water, medical, shelter, hazards)
5. **Local AI**: Offline summarization, translation, procedure guidance, decision support
6. **External Radio**: Long-range LoRa/FSK nodes extending the mesh beyond phone range
7. **SDK**: Third-party developers building Beacon-compatible applications
8. **Beacon OS**: Hardened Android/Linux distribution for dedicated nodes

---

## Guiding Principles

### 1. Offline-First
**Core functionality must not depend on the Internet.**
- All critical features work in airplane mode
- Cloud is an enhancement, never a requirement
- Data sync is opportunistic, not blocking

### 2. Energy-First
**Every background process must justify its energy consumption.**
- Radio duty cycling is mandatory
- Batch communication over continuous scanning
- Power-aware routing prefers high-battery nodes
- Survival modes gracefully degrade functionality

### 3. Security-by-Design
**Communication must be secure by default.**
- End-to-end encryption for all messages
- Identity based on cryptographic keys, not phone numbers
- Forward secrecy and replay protection
- No custom cryptography—use established primitives

### 4. Modularity
**Components must be independently replaceable.**
- Clean abstraction layers between core, mesh, radio, UI
- SDK allows alternative implementations
- Hardware abstraction enables new radio types

### 5. Evidence-Based Engineering
**Technical decisions require research, measurement, or documented reasoning.**
- No premature optimization—measure first
- No premature hardware—validate software first
- Competitor analysis before building
- Performance claims require benchmarks

### 6. Open Source & Interoperability
**Prefer open standards and permissively licensed technologies.**
- Protocol specifications published openly
- Reference implementations under MIT/Apache-2.0
- Compatibility with existing mesh projects where feasible
- Community governance model

---

## Success Metrics

### Milestone 1 (Foundation) — Current
- [ ] Vision, PRD, Architecture, SRS approved
- [ ] ADR-0001, ADR-0002 resolved
- [ ] SDK interfaces defined
- [ ] Simulator runs basic topologies
- [ ] Docs pipeline deploys on every PR

### Milestone 2 (Communication MVP)
- [ ] Two Android phones exchange messages via BLE (Internet OFF)
- [ ] Message persistence and delivery status
- [ ] Basic peer discovery and presence
- [ ] Battery consumption < 5%/hour idle

### Milestone 3 (Mesh Networking)
- [ ] 4+ node multi-hop message delivery
- [ ] Store-and-forward across intermittent connectivity
- [ ] Duplicate detection and TTL enforcement
- [ ] Routing algorithm benchmarked in simulator

### Milestone 4 (Offline Maps)
- [ ] Vector tiles render offline at 60fps
- [ ] POI search without network
- [ ] Route calculation on-device
- [ ] Community markers sync via mesh

### Milestone 5 (Dashboard)
- [ ] Situational awareness UI (resources, alerts, network)
- [ ] Distributed data aggregation
- [ ] Cross-platform (Android + Desktop)

### Milestone 6 (Local AI)
- [ ] Offline LLM runs on-device (quantized)
- [ ] Summarization, translation, triage assistance
- [ ] Non-critical: networking works without AI

### Milestone 7 (Radio Hardware)
- [ ] ESP32 + LoRa prototype communicates with phone
- [ ] Range measured in real conditions
- [ ] Power consumption characterized
- [ ] Enclosure design published

### Milestone 8 (SDK)
- [ ] Stable API for third-party apps
- [ ] Multi-language bindings (Kotlin, Swift, JS)
- [ ] Plugin architecture for transports

### Milestone 9 (Beacon OS)
- [ ] Hardened Android build for dedicated nodes
- [ ] OTA updates via mesh
- [ ] Headless operation on SBCs (Pi, etc.)

---

## Non-Goals (Explicitly Out of Scope)

| Non-Goal | Rationale |
|----------|-----------|
| Real-time voice/video over mesh | Bandwidth incompatible with low-power radio |
| Blockchain/cryptocurrency integration | Adds complexity without disaster utility |
| Replacement for Internet | Complementary, not competitive |
| Centralized cloud services | Contradicts offline-first principle |
| Custom cryptographic primitives | Security risk; use audited libraries |
| iOS-only or proprietary platforms | Excludes majority of global users |
| Military/combat applications | Humanitarian focus; dual-use policy |

---

## Stakeholders

| Stakeholder | Interest | Engagement |
|-------------|----------|------------|
| **End Users** (affected populations) | Life-saving communication | Primary design driver |
| **First Responders** (SAR, EMS, Fire) | Coordination, situational awareness | Co-design partners |
| **NGOs / Humanitarian Orgs** | Field deployment, training | Distribution partners |
| **Open Source Community** | Contribution, extension | Governance participants |
| **Researchers** (DTN, mesh, HCI) | Validation, publication | Technical advisors |
| **Hardware Makers** | Compatible devices | Ecosystem partners |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Regulatory barriers to LoRa/ISM use | Medium | High | Frequency planning, regional variants |
| Battery life insufficient in practice | High | High | Aggressive power management, measurement |
| Mesh routing doesn't scale | Medium | High | Simulator validation before hardware |
| Adoption too low for network effect | Medium | High | Android-first, zero-cost entry |
| Security vulnerabilities discovered | Medium | High | Formal audit, responsible disclosure |
| Key maintainer burnout | Medium | High | Distributed governance, documentation |
| Competing standards fragment ecosystem | Medium | Medium | Open protocol, interoperability focus |

---

## Relationship to Other Documents

| Document | Relationship |
|----------|--------------|
| `ROADMAP.md` | Tactical milestones derived from this vision |
| `docs/prd/prd.md` | Product requirements implementing this vision |
| `docs/architecture/architecture.md` | Technical architecture realizing this vision |
| `docs/srs/srs.md` | Detailed specifications for implementation |
| `adr/` | Architectural decisions made in pursuit of this vision |
| `docs/protocol/protocol.md` | Wire protocol derived from this vision |

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 1.0.0 | Initial approved vision document | Project Beacon Core Team |

---

## Approval

This document has been reviewed and approved by the Project Beacon maintainers as the guiding vision for all subsequent development.

**Status: APPROVED** ✅