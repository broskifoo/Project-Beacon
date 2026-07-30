# Product Requirements Document (PRD) Template

## Document Metadata

* **Document ID:** `DOC-PRD-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
This PRD outlines the specific features, user experiences, and functional requirements for the Project Beacon platform. It guides product design, UI/UX workflow development, and engineers on what to build.

### Scope
This covers the user-facing mobile client, node administration dashboard, core system states, and external hardware API requirements. Technical protocols, message formats, and specific hardware layout plans are out of scope (covered in SRS and specifications).

---

## Table of Contents

1. [Product Objectives & Goals](#1-product-objectives--goals)
2. [User Persona Definitions](#2-user-persona-definitions)
3. [User Journeys & Stories](#3-user-journeys--stories)
4. [Functional Requirements (MoSCoW)](#4-functional-requirements-moscow)
5. [UX/UI Requirements & Wireframes](#5-uxui-requirements--wireframes)
6. [Open Product Questions](#6-open-product-questions)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Product Objectives & Goals
*Identify what success looks like for the product.*

#### 1.1 Core Objectives
* *Goal 1: Enable mesh message delivery under 5 seconds between nodes.*
* *Goal 2: Provide offline map rendering with zero network latency.*

#### 1.2 Out of Scope
* *Non-goal: Real-time audio/video streaming over low-bandwidth radio channels.*

---

### 2. User Persona Definitions
*Detailed descriptions of target users.*

* **Persona A: Emergency Manager (S&R Lead)**
  * *Needs:* Dynamic map coordination, group broadcast.
  * *Frustrations:* Fragmented communications, lack of unified location logs.
* **Persona B: Impacted Citizen**
  * *Needs:* Simple SOS button, safe routing maps, battery-efficient operation.
  * *Frustrations:* Confusing user interfaces, fast battery depletion.

---

### 3. User Journeys & Stories
*Define how users interact with the system.*

#### 3.1 User Story 1: Emergency SOS Broadcast
* *As a:* Disconnected citizen in a flood zone.
* *I want to:* Send an encrypted, low-bandwidth distress signal containing my GPS coordinates.
* *So that:* Nearby rescue teams can view my location on their offline dashboard.

#### 3.2 User Story 2: Group Route Sharing
* *As a:* Search and rescue team lead.
* *I want to:* Broadcast a safe search route map overlay to my team.
* *So that:* Everyone coordinates their search area without cell coverage.

---

### 4. Functional Requirements (MoSCoW)
*Prioritized list of features.*

#### 4.1 Must Have
* *Req-01: Bluetooth LE node pairing for mobile device connection.*
* *Req-02: Offline vector map tile storage and display.*

#### 4.2 Should Have
* *Req-03: Local message prioritization (e.g., triage SOS over chat).*

#### 4.3 Could Have
* *Req-04: Automated network range alerts.*

#### 4.4 Won't Have (For Now)
* *Req-05: Desktop client support (focus on Mobile & Dashboard).*

---

### 5. UX/UI Requirements & Wireframes
*Visual flow requirements. (See also [UI/UX Specification](docs/ui-ux/ui-ux_template.md)).*

* **Screen A: Hub Map:** Focus on map visibility and nearby peer locator.
* **Screen B: Emergency Panel:** Large SOS button with confirmation count down to prevent false alarms.

---

### 6. Open Product Questions
*List unresolved questions (e.g., "Should we support iOS/CoreBluetooth alongside Android?").*

---

## References

* *[Ref-01] Placeholder Reference 1*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
