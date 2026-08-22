# Security Specification

## Document Metadata

* **Document ID:** `DOC-SEC-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the security architecture, threat model, cryptographic primitives, and security requirements for Project Beacon.

---

## Threat Model

### Assets to Protect

| Asset | Sensitivity | Impact if Compromised |
|-------|-------------|----------------------|
| Message Content | HIGH | Privacy violation, operational security |
| Peer Identities | HIGH | Tracking, impersonation, Sybil attacks |
| Location Data | HIGH | Physical safety, stalking |
| Network Topology | MEDIUM | Traffic analysis, targeting |
| Encryption Keys | CRITICAL | Total system compromise |
| Resource Reports | MEDIUM | Misinformation, resource diversion |

### Adversaries

| Adversary | Capabilities | Motivation |
|-----------|--------------|------------|
| **Passive Eavesdropper** | Monitor radio spectrum, capture packets | Intelligence gathering |
| **Active Attacker** | Inject/modify/drop packets, replay | Disruption, misinformation |
| **Malicious Node** | Participates in mesh but lies | Resource exhaustion, blackhole |
| **Compromised Device** | Full control of legitimate node | Key extraction, impersonation |
| **Sybil Attacker** | Creates many fake identities | Network partition, reputation gaming |
| **Resource Exhaustion** | Floods network, drains batteries | Denial of service |

### Trust Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                     TRUSTED COMPUTING BASE                   │
├─────────────────────────────────────────────────────────────┤
│  • Android Keystore / iOS Secure Enclave                    │
│  • Beacon Core (message handling, encryption)               │
│  • Identity Management                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      UNTRUSTED                               │
├─────────────────────────────────────────────────────────────┤
│  • Radio Transport (BLE, Wi-Fi, LoRa)                       │
│  • Mesh Network (other nodes)                               │
│  • External Hardware (ESP32 radio nodes)                    │
│  • User Applications                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Goals

| Goal | Priority | Mechanism |
|------|----------|-----------|
| **Confidentiality** | CRITICAL | ChaCha20-Poly1305 E2E encryption |
| **Integrity** | CRITICAL | AEAD authentication tags |
| **Authentication** | CRITICAL | Ed25519 signatures per message |
| **Forward Secrecy** | HIGH | Ephemeral X25519 per session |
| **Replay Protection** | HIGH | Nonce + timestamp + sequence |
| **Identity Binding** | HIGH | Keys = identity, no central authority |
| **Metadata Protection** | MEDIUM | Encrypted routing headers (future) |
| **Traffic Analysis Resistance** | MEDIUM | Cover traffic, fixed-size packets (future) |
| **Compromise Recovery** | MEDIUM | Key rotation, identity revocation |

---

## Cryptographic Primitives

### Approved Algorithms

| Purpose | Algorithm | Parameters | Library |
|---------|-----------|------------|---------|
| **Identity/Signing** | Ed25519 | PureEdDSA, SHA-512 | libsodium / Android Keystore |
| **Key Agreement** | X25519 | Curve25519 ECDH | libsodium / Android Keystore |
| **Encryption** | ChaCha20-Poly1305 | 256-bit key, 96-bit nonce | libsodium / Android Keystore |
| **Key Derivation** | HKDF-SHA256 | salt="beacon-v1" | libsodium / Android Keystore |
| **Hashing** | SHA-256 | — | Platform |
| **Random** | ChaCha20 | CSPRNG | Platform CSPRNG |

### Key Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    ROOT OF TRUST                             │
│              (Hardware Secure Element)                       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│   Identity Key Pair     │     │   Master Encryption Key │
│     (Ed25519)           │     │     (AES-256-GCM)       │
│  - Signing              │     │  - Storage encryption   │
│  - Verification         │     │  - Key wrapping         │
└─────────────────────────┘     └─────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────┐
│              EPHEMERAL SESSION KEYS                          │
│  Per-session X25519 → HKDF → ChaCha20-Poly1305 keys          │
│  Forward secrecy: compromise of long-term keys ≠ past traffic│
└─────────────────────────────────────────────────────────────┘
```

### Key Lifecycle

| Phase | Action | Trigger |
|-------|--------|---------|
| **Generation** | Ed25519 keypair in Secure Element | First app launch |
| **Backup** | Encrypted export (user passphrase) | User initiates |
| **Rotation** | New keypair, cross-sign old | Periodic (90 days) or compromise |
| **Revocation** | Publish revocation via mesh | Key compromise detected |
| **Destruction** | Secure delete from enclave | User request / factory reset |

---

## Message Security

### Per-Message Protection

```
Sender                                    Receiver
  │                                        │
  ├─ 1. Create message payload            │
  ├─ 2. Generate ephemeral X25519 keypair │
  ├─ 3. ECDH with recipient's X25519 pub  │
  ├─ 4. HKDF → session key                │
  ├─ 5. Encrypt payload (ChaCha20-Poly1305)│
  ├─ 6. Sign: Ed25519(routing || cipher)  │
  ├─ 7. Assemble frame                    │
  │                                        │
  ▼                                        ▼
┌──────────────────────────────────────────────────────────┐
│                    WIRE FORMAT                            │
├──────────────────────────────────────────────────────────┤
│ Routing Header (84 bytes, plaintext)                     │
│ Ciphertext (variable)                                    │
│ Auth Tag (16 bytes)                                      │
│ Signature (64 bytes, if FLAGS.SIGNED)                    │
└──────────────────────────────────────────────────────────┘
```

### Nonce Construction

```
Nonce (12 bytes) = Message ID (8 bytes) || Counter (4 bytes)

- Message ID: UUIDv7 (time-ordered, 48-bit timestamp + 80-bit random)
- Counter: Incremented per fragment (0 for single-frame messages)
```

### Replay Protection

1. **Nonce uniqueness**: UUIDv7 + counter ensures unique nonces
2. **Timestamp validation**: Reject messages with timestamp > 5 min skew
3. **Sequence numbers**: Per-peer monotonic counter for session
4. **Bloom filter**: Recent message IDs (last 10,000) for duplicate detection

---

## Transport Security

### BLE

| Layer | Protection |
|-------|------------|
| **Link Layer** | AES-CCM (BLE 4.2+), optional |
| **L2CAP CoC** | Application-layer E2E (our protocol) |
| **Pairing** | Not used (we manage our own keys) |

### Wi-Fi Direct

| Layer | Protection |
|-------|------------|
| **WPA2/WPA3** | Link-layer encryption (opportunistic) |
| **TCP** | Application-layer E2E (our protocol) |

### LoRa

| Layer | Protection |
|-------|------------|
| **PHY** | None (spreading provides obscurity) |
| **Application** | Full E2E encryption (our protocol) |

---

## Storage Security

### At-Rest Encryption

- **Database**: SQLCipher (AES-256-GCM)
- **Key**: Derived from user passphrase + hardware-bound key
- **Key Storage**: Android Keystore / iOS Secure Enclave

### Key Storage

| Key Type | Storage | Protection |
|----------|---------|------------|
| Identity Private Key | Secure Element | Non-extractable, auth required |
| Ephemeral Session Keys | Memory (encrypted) | Zeroized on session end |
| Master Storage Key | Secure Element | Wrapped by user passphrase |
| Peer Public Keys | Encrypted DB | Authenticated encryption |

---

## Network-Level Security

### Peer Authentication

```
Every message carries:
  - Source Identity (Ed25519 public key, 32 bytes)
  - Signature over routing header + ciphertext
  
Verification:
  1. Check signature with source public key
  2. Verify source is in trust store or TOFU
  3. Check replay protection
  4. Decrypt with session key
```

### Trust on First Use (TOFU)

1. First message from unknown peer → store public key
2. User can verify fingerprint out-of-band
3. Mark as "trusted" or "untrusted"
4. Untrusted peers: messages quarantined

### Sybil Resistance

- **Identity cost**: Key generation requires secure element (rate-limited)
- **Proof-of-work**: Optional for high-security modes
- **Reputation**: Track peer behavior, penalize anomalies
- **Social graph**: Trust propagation from known contacts

---

## Privacy Considerations

### Data Minimization

- No persistent identifiers in routing headers (use ephemeral keys)
- Location only in payload (encrypted)
- No phone numbers, emails, or personal data required

### Metadata Protection (Future)

- **Encrypted routing headers**: Encrypt dest/src with network-wide key
- **Cover traffic**: Send dummy packets to mask real traffic
- **Packet padding**: Fixed-size packets to hide message length
- **Timing obfuscation**: Random delays before forwarding

### Location Privacy

- **User control**: Explicit opt-in for location sharing
- **Precision reduction**: Fuzzed coordinates for non-critical messages
- **Ephemeral IDs**: Rotate broadcast identifiers periodically

---

## Security Testing Requirements

| Test | Frequency | Method |
|------|-----------|--------|
| **Cryptographic validation** | Every release | NIST CAVP vectors, Wycheproof |
| **Fuzzing** | CI + periodic | AFL++ on parsers, AFLNet on protocol |
| **Penetration testing** | Quarterly | External audit |
| **Side-channel analysis** | Annual | Timing, power, EM analysis |
| **Dependency scanning** | CI | Cargo audit, npm audit, Snyk |
| **SAST/DAST** | CI | CodeQL, Semgrep |

---

## Incident Response

### Key Compromise

1. **Detect**: Unusual signatures, failed verifications, user report
2. **Revoke**: Broadcast revocation certificate via mesh
3. **Rotate**: Generate new identity key, cross-sign
4. **Notify**: Alert contacts via secure channel
5. **Recover**: Re-establish sessions with new key

### Vulnerability Disclosure

- **Coordinated disclosure**: 90-day timeline
- **Security contact**: security@projectbeacon.org
- **Bounty program**: TBD

---

## Compliance & Standards

| Standard | Relevance | Status |
|----------|-----------|--------|
| NIST SP 800-57 | Key management | Aligned |
| NIST SP 800-38D | AES-GCM / ChaCha20-Poly1305 | Compliant |
| RFC 8032 | Ed25519 | Compliant |
| RFC 7748 | X25519 | Compliant |
| RFC 5869 | HKDF | Compliant |
| Signal Protocol | Double Ratchet (future) | Planned |

---

## References

* [Architecture Overview](../architecture/architecture.md)
* [Protocol Specification](../protocol/protocol.md)
* [ADR-0001: BLE Discovery](../adr/ADR-0001-ble-discovery.md)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |