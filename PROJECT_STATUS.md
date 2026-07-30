# Project Status Dashboard

This document provides a real-time, high-level overview of the current developmental state of Project Beacon.

---

## Project Metadata

| Attribute | Value / Status |
|---|---|
| **Current Version** | `v0.1.0-alpha` |
| **Current Milestone** | **Milestone 1 — Foundation** |
| **Release Track** | Pre-alpha / Architectural Bootstrap |
| **Last Updated** | *Placeholder: YYYY-MM-DD* |

---

## Active Documents

The following documents are currently being drafted, reviewed, or actively maintained:

| Document ID | Title | Status | Primary Owner | Target Completion |
|---|---|---|---|---|
| `DOC-VISION-001` | [Vision Document](docs/vision/vision_template.md) | Draft | *Placeholder* | *Placeholder* |
| `DOC-PRD-001` | [Product Requirements Document (PRD)](docs/prd/prd_template.md) | Draft | *Placeholder* | *Placeholder* |
| `DOC-SRS-001` | [Software Requirements Specification (SRS)](docs/srs/srs_template.md) | Draft | *Placeholder* | *Placeholder* |
| `DOC-ARCH-001` | [Architecture Overview](docs/architecture/architecture_template.md) | Draft | *Placeholder* | *Placeholder* |

---

## Active Research

Current research initiatives exploring technology limits, algorithms, or hardware designs:

| Research ID | Topic / Goal | Status | Lead Researcher |
|---|---|---|---|
| `RES-MESH-001` | *Placeholder: Evaluation of LoRa mesh routing protocols (e.g., Reticulum, Meshtastic)* | Active | *Placeholder* |
| `RES-MAPS-001` | *Placeholder: Memory-efficient offline vector tile rendering on mobile* | Active | *Placeholder* |
| `RES-HW-001` | *Placeholder: RF power consumption optimization for solar-powered beacon units* | Active | *Placeholder* |

---

## Architecture Decision Records (ADRs)

Track of major architectural design decisions. See the `/adr` directory for template and specific files.

### Open ADRs (Under Discussion)
* **ADR-0001:** *Placeholder: Choice of Primary Low-Power Mesh Routing Algorithm* (Under Review)
* **ADR-0002:** *Placeholder: Offline GIS Database Engine Selection (SQLite vs. DuckDB vs. Custom flat files)* (Draft)

### Completed ADRs (Approved)
* **ADR-0000:** [ADR Template](adr/ADR-0000-template.md) (Standardized format approved)

---

## Next Objectives

Immediate technical and organizational objectives for the upcoming development cycles:

### Milestone 1 — Foundation (Current Sprint Focus)
* [ ] Finalize baseline hardware requirement specifications.
* [ ] Draft initial core API interfaces in `beacon-sdk`.
* [ ] Approve `DOC-VISION-001` and `DOC-PRD-001`.
* [ ] Set up simulation environments in `beacon-simulator`.
* [ ] Establish the automated docs builds pipeline (`workflows/docs.yml`).
