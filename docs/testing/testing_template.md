# Testing Strategy Template

## Document Metadata

* **Document ID:** `DOC-TEST-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
The Testing Strategy defines the verification methodologies, frameworks, levels, and quality gates for Project Beacon. It establishes a rigorous pipeline to ensure code reliability and protocol stability under harsh real-world deployments.

### Scope
This testing strategy encompasses Unit Testing, Integration Testing, Hardware-in-the-Loop (HIL) Testing, Network Simulator Testing (using `beacon-simulator`), and Field Testing.

---

## Table of Contents

1. [Testing Levels & Frameworks](#1-testing-levels--frameworks)
2. [Test Coverage Targets](#2-test-coverage-targets)
3. [Hardware-In-The-Loop (HIL) Testing](#3-hardware-in-the-loop-hil-testing)
4. [Large-Scale Network Simulation](#4-large-scale-network-simulation)
5. [Field Testing Guidelines](#5-field-testing-guidelines)
6. [Continuous Integration (CI) Checks](#6-continuous-integration-ci-checks)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Testing Levels & Frameworks

* **Unit Testing:** Focuses on pure functions, data serialization, and routing math. 
  * *Frameworks:* PyTest (Python), JUnit/MockK (Kotlin), SwiftTesting (iOS), Catch2 (C++).
* **Integration Testing:** Tests communication between `beacon-core` databases and `beacon-mesh` logic.
* **System Testing:** End-to-end user flows, such as typing a message in the dashboard and verifying it arrives at a simulated companion node.

---

### 2. Test Coverage Targets

We enforce strict test coverage targets on our core subsystems:

| Subproject | Minimum Line Coverage | Minimum Branch Coverage |
|---|---|---|
| `beacon-core` | 80% | 75% |
| `beacon-mesh` | 90% | 85% |
| `beacon-radio` | 75% | 70% |
| `beacon-sdk` | 85% | 80% |

---

### 3. Hardware-In-The-Loop (HIL) Testing

HIL testing validates firmware compatibility on actual microcontroller hardware before deployments:

```text
[ Test Orchestrator (PC) ] --- (Commands over USB/JTAG) ---> [ Node Under Test ]
             |                                                       |
             v (Monitors RF spectrum)                                v (RF Out)
     [ RF Attenuator / Analyzer ] <==================================+
```

* *Objective:* Validate SPI connection, transceiver power levels, and clock timings on physical silicon.

---

### 4. Large-Scale Network Simulation

Using `beacon-simulator` to spin up 100+ virtual nodes to evaluate:
* Routing table convergence rates under high packet loss.
* Channel congestion and contention behavior (CSMA/CA or TDMA metrics).
* Self-healing capabilities when critical gateway nodes are disconnected.

---

### 5. Field Testing Guidelines

Guidelines for real-world validation of radio links:
* **Line-of-Sight (LoS) range testing:** Documenting GPS coordinates, RSSI, and packet delivery ratios at set distances (1km, 5km, 10km).
* **Urban/Indoor penetration tests:** Evaluating signal degradation through concrete, glass, and vegetation.

---

### 6. Continuous Integration (CI) Checks

* Static code analysis (Linters, security scanners like Coverity or SonarQube).
* Automated unit testing on every Pull Request.
* Merge block on coverage regressions.

---

## References

* *[Ref-01] ISO/IEC/IEEE 29119 Software Testing Standards*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
