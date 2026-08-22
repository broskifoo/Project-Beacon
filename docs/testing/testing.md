# Testing Strategy

## Document Metadata

* **Document ID:** `DOC-TEST-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the testing strategy, methodologies, tools, and acceptance criteria for Project Beacon across all components.

---

## Testing Philosophy

| Principle | Description |
|-----------|-------------|
| **Test Pyramid** | 70% unit, 20% integration, 10% E2E |
| **Shift Left** | Test early, test often; CI on every PR |
| **Real Conditions** | Test on real devices, real radios, real networks |
| **Automation First** | Manual testing only for exploratory/UX |
| **Measurement Over Assertion** | Performance, battery, latency must be measured |
| **Security by Default** | Crypto validation, fuzzing, penetration testing |

---

## Test Levels

### Level 1: Unit Tests (Target: >80% Coverage)

| Component | Framework | Scope |
|-----------|-----------|-------|
| **beacon-core** | JUnit 5 + MockK | Business logic, state machines, serialization |
| **beacon-mesh** | JUnit 5 + MockK | Routing algorithms, bundle handling, TTL |
| **beacon-radio** | JUnit 5 + Robolectric | Transport drivers, frame encoding |
| **beacon-sdk** | JUnit 5 + MockK | API contracts, error handling |
| **beacon-simulator** | pytest | Models, propagation, mobility, routing |
| **Rust Core** | cargo test | Crypto, protocol, data structures |

**Requirements:**
- Run in < 30 seconds
- No external dependencies (no network, no hardware)
- Deterministic, hermetic
- Property-based testing for protocols (proptest, hypothesis)

### Level 2: Integration Tests

| Test Suite | Description | Environment |
|------------|-------------|-------------|
| **SDK Integration** | Full API surface against mock transport | CI (Robolectric/AndroidTest) |
| **Mesh Protocol** | Multi-node routing, store-forward | Simulator (10-100 nodes) |
| **Transport Drivers** | BLE/WiFi-Direct/LoRa frame exchange | Device farm (2-4 devices) |
| **Storage** | SQLCipher encryption, migration, corruption | CI + Device |
| **Identity** | Key gen, rotation, revocation, import/export | CI + Device |
| **Maps** | Tile rendering, POI search, routing | CI (headless) + Device |

### Level 3: End-to-End / Field Tests

| Test Category | Scenario | Devices | Metrics |
|---------------|----------|---------|---------|
| **Communication MVP** | 2 phones, BLE only, Internet OFF | 2 Android | Latency, delivery rate, battery |
| **Multi-hop Mesh** | 4+ phones, chain topology | 4-8 Android | Hop count, latency, success rate |
| **Store-and-Forward** | Partitioned network, delayed delivery | 4-6 Android | Delivery after reconnect |
| **Battery Life** | 24h background, each power mode | 4 Android | %/hour drain |
| **Range Testing** | Open field, urban, indoor | 2-4 + Radio nodes | RSSI vs distance, packet loss |
| **Interop** | Android + Radio node + Gateway | Mixed | Cross-platform delivery |
| **Stress** | 50+ nodes, high traffic | Simulator | Throughput, queue depths, drops |

### Level 4: Security Testing

| Test Type | Tool | Frequency | Scope |
|-----------|------|-----------|-------|
| **Crypto Validation** | Wycheproof, NIST CAVP | Every release | All primitives |
| **Fuzzing** | AFL++ (parsers), AFLNet (protocol) | CI + Weekly | Frame parsing, CBOR, handlers |
| **Static Analysis** | CodeQL, Semgrep, Clippy | Every PR | Code patterns, secrets |
| **Dependency Scan** | Cargo audit, npm audit, Snyk | Daily | Supply chain |
| **Penetration Test** | External audit | Quarterly | Full stack |
| **Side-Channel** | Timing, power analysis | Annual | Key operations |

---

## Test Infrastructure

### CI Pipeline (GitHub Actions)

```yaml
# .github/workflows/test.yml
jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - checkout
      - setup: Java 21, Android SDK, Rust, Python
      - run: ./gradlew :beacon-core:test :beacon-mesh:test :beacon-sdk:kotlin:test
      - run: cargo test --workspace
      - run: pytest beacon-simulator/python/tests/
      - upload: coverage reports

  integration-test:
    needs: unit-test
    runs-on: macos-latest  # For Android emulator
    steps:
      - checkout
      - setup: Android emulator API 34
      - run: ./gradlew connectedAndroidTest
      - run: python -m pytest beacon-simulator/python/tests/integration/

  security-test:
    runs-on: ubuntu-latest
    steps:
      - checkout
      - run: cargo audit
      - run: npm audit
      - run: python -m pip-audit
      - run: codeql analyze

  field-test:
    if: github.event_name == 'workflow_dispatch'
    runs-on: self-hosted  # Physical device lab
    steps:
      - deploy to device lab
      - run automated field test suite
      - collect metrics, upload
```

### Device Lab

| Resource | Specification |
|----------|---------------|
| **Phones** | 20+ Android devices (API 24-34), varied OEMs |
| **Radio Nodes** | 10+ BRN-1 prototypes |
| **Gateways** | 3x Raspberry Pi 4 + SX1302 |
| **Environment** | Faraday cage, anechoic chamber (optional) |
| **Automation** | ADB, uiautomator2, custom harness |

### Simulation Cluster

| Resource | Specification |
|----------|---------------|
| **Nodes** | 1000+ simulated nodes |
| **Scenarios** | Urban, rural, disaster, mobile, static |
| **Output** | Metrics CSV, topology JSON, event logs |
| **Analysis** | Jupyter notebooks, Grafana dashboards |

---

## Test Data Management

### Synthetic Data Generators

```python
# Test data factories
def create_message(
    msg_type: MessageType = MessageType.TEXT,
    priority: MessagePriority = MessagePriority.NORMAL,
    payload_size: int = 100,
    **overrides
) -> Message:
    """Generate realistic test message."""
    ...

def create_topology(
    num_nodes: int = 50,
    area_km2: float = 1.0,
    density: str = "urban"
) -> NetworkTopology:
    """Generate realistic mesh topology."""
    ...

def create_peer(
    transport: TransportType = TransportType.BLE,
    battery: int = 80,
    **overrides
) -> Peer:
    """Generate test peer."""
    ...
```

### Golden Files

- **Protocol vectors**: Known-good frame encodings/decodings
- **Crypto vectors**: NIST/Wycheproof test vectors
- **Topology snapshots**: Reference network states
- **Map tiles**: Test MBTiles for rendering

---

## Acceptance Criteria by Milestone

### Milestone 1: Foundation (Current)

- [ ] All unit tests pass (>80% coverage)
- [ ] Docs build and deploy
- [ ] ADRs reviewed and approved
- [ ] SDK interfaces compile on all targets
- [ ] Simulator runs basic scenarios
- [ ] CI pipeline green

### Milestone 2: Communication MVP

- [ ] **TC-COMM-001**: Two phones exchange TEXT message via BLE (Internet OFF)
- [ ] **TC-COMM-002**: SOS message sent, received, acknowledged
- [ ] **TC-COMM-003**: Message persists across app kill/reboot
- [ ] **TC-COMM-004**: Delivery status updates correctly (QUEUED→SENT→DELIVERED→ACKED)
- [ ] **TC-COMM-005**: Priority queue ordering (CRITICAL before NORMAL)
- [ ] **TC-COMM-006**: Fragmentation/reassembly for large messages
- [ ] **TC-COMM-007**: Battery drain < 5%/hour idle (NORMAL mode)
- [ ] **TC-COMM-008**: App runs 24h without crash/ANR
- [ ] **TC-SEC-001**: All messages encrypted (verify with sniffer)
- [ ] **TC-SEC-002**: Replay attack rejected
- [ ] **TC-SEC-003**: Invalid signature rejected

### Milestone 3: Mesh Networking

- [ ] **TC-MESH-001**: 4-node chain delivers message (A→B→C→D)
- [ ] **TC-MESH-002**: Store-and-forward across 5-min partition
- [ ] **TC-MESH-003**: Duplicate detection works (no double-delivery)
- [ ] **TC-MESH-004**: TTL enforcement (drop at 0)
- [ ] **TC-MESH-005**: Probabilistic forwarding reduces redundancy
- [ ] **TC-MESH-006**: Geographic routing finds shorter paths
- [ ] **TC-MESH-007**: Simulator validates routing at 100/1000/10000 nodes
- [ ] **TC-POWER-001**: Energy-aware routing prefers high-battery nodes

### Milestone 4: Offline Maps

- [ ] **TC-MAP-001**: Vector tiles render at 60fps (pan/zoom)
- [ ] **TC-MAP-002**: POI search < 100ms for 10k POIs
- [ ] **TC-MAP-003**: Route calculation < 500ms for 10km
- [ ] **TC-MAP-004**: Community marker sync via mesh
- [ ] **TC-MAP-005**: Map region export/import (MBTiles)
- [ ] **TC-MAP-006**: Confidence scoring displayed correctly

### Milestone 5: Dashboard

- [ ] **TC-DASH-001**: Real-time topology visualization
- [ ] **TC-DASH-002**: Resource CRUD with verification
- [ ] **TC-DASH-003**: Alert broadcast with geo-targeting
- [ ] **TC-DASH-004**: Multi-node fleet view
- [ ] **TC-DASH-005**: Historical analytics (delivery, battery, latency)

---

## Performance Benchmarks

### Continuous Benchmarking

```bash
# Run on every PR (GitHub Actions)
./gradlew :beacon-core:benchmark :beacon-mesh:benchmark
cargo bench --workspace
pytest beacon-simulator/python/tests/benchmarks/
```

### Key Benchmarks

| Benchmark | Target | Measurement |
|-----------|--------|-------------|
| **Frame encode/decode** | < 10 µs | 100k iterations |
| **ChaCha20-Poly1305 encrypt** | < 5 µs/KB | 1MB data |
| **Ed25519 sign/verify** | < 1 ms / < 0.5 ms | 10k ops |
| **X25519 key agreement** | < 2 ms | 10k ops |
| **SQLite insert (message)** | < 1 ms | 10k messages |
| **SpatiaLite bbox query** | < 10 ms | 10k POIs |
| **Map tile decode** | < 16 ms | 256×256 tile |
| **Routing decision** | < 1 ms | 50 neighbors |

---

## Test Reporting

### Metrics Dashboard (Grafana)

| Panel | Query | Alert Threshold |
|-------|-------|-----------------|
| Unit Test Pass Rate | `passed / total` | < 95% |
| Coverage | `line_coverage` | < 80% |
| Integration Pass Rate | `passed / total` | < 90% |
| Field Test Delivery Rate | `delivered / sent` | < 90% (direct) |
| Battery Drain (NORMAL) | `avg(%/hour)` | > 5% |
| Binary Size (APK) | `apk_size_mb` | > 50 MB |
| Build Time | `duration_minutes` | > 15 min |

### Test Reports

- **JUnit XML**: CI ingestion
- **Allure Report**: Rich HTML with screenshots
- **Coverage**: JaCoCo (Java/Kotlin), llvm-cov (Rust), coverage.py (Python)
- **Benchmark**: Criterion.rs, JMH, pytest-benchmark

---

## Test Case Template

```markdown
## TC-<AREA>-<NNN>: <Brief Description>

**Milestone**: M<X>
**Priority**: MUST/SHOULD/COULD
**Type**: Unit/Integration/E2E/Security/Performance

### Preconditions
- Device state, network config, test data

### Steps
1. Action
2. Action
3. Verify

### Expected Result
- Specific observable outcome

### Metrics Collected
- Latency, battery, throughput, etc.

### Pass Criteria
- Quantitative thresholds

### Related Requirements
- FR-<ID>, NFR-<ID>, ADR-<ID>
```

---

## References

* [Architecture Overview](../architecture/architecture.md)
* [PRD](../prd/prd.md)
* [SRS](../srs/srs.md)
* [Security Specification](../security/security.md)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |