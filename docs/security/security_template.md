# Security Specification Template

## Document Metadata

* **Document ID:** `DOC-SEC-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
This Security Specification defines the cryptographic primitives, key management processes, authentication schemes, and threat vectors for the Project Beacon network. It guides developers on security implementation patterns to protect user safety and network integrity.

### Scope
This covers link-layer cryptography, end-to-end user encryption, authentication models, firmware validation, and storage security. It defines expectations for both mobile software and physical hardware configurations.

---

## Table of Contents

1. [Threat Model & Risk Profile](#1-threat-model--risk-profile)
2. [Cryptographic Primitives](#2-cryptographic-primitives)
3. [Key Exchange & Trust Establishment](#3-key-exchange--trust-establishment)
4. [Link & End-to-End Encryption](#4-link--end-to-end-encryption)
5. [Firmware & Hardware Security](#5-firmware--hardware-security)
6. [Security Guidelines for Contributors](#6-security-guidelines-for-contributors)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Threat Model & Risk Profile

#### 1.1 Threat Actors
* *Eavesdroppers:* Capturing plaintext radio packets.
* *Malicious Nodes:* Attempting routing loops or Sybil attacks.
* *Tampering:* Intercepting and editing packets in transit.

#### 1.2 Out of Scope Threats
* *Physical destruction:* Overwhelming node hardware destruction.
* *Jamming:* Sustained broad-spectrum RF interference.

---

### 2. Cryptographic Primitives

The system relies on lightweight, fast primitives suitable for microcontrollers:

* **Symmetric Encryption:** ChaCha20-Poly1305.
* **Asymmetric Encryption:** Curve25519 (X25519) for key exchange.
* **Digital Signatures:** Ed25519.
* **Hashing:** BLAKE2b or SHA-256.

---

### 3. Key Exchange & Trust Establishment

#### 3.1 Initial Pairing (Bluetooth / QR Code)
*Describe how a new phone pairs with a physical transceiver and shares local secrets.*

#### 3.2 Over-The-Air Key Exchange (ECDH)
*Flow diagram of Curve25519 handshake between nodes to establish a session key.*

---

### 4. Link & End-to-End Encryption

```text
[ Citizen Device ] ---> (E2EE Payload - Encrypted with recipient's public key)
      |
      v
[ Local Transceiver ] ---> (Link Payload - Signed & optionally encrypted per hop)
```

* **End-to-End Encryption (E2EE):** Encrypts message body (e.g. text/location). Transmitting nodes routing the packet cannot view details.
* **Link Encryption:** Hides routing information from passive eavesdroppers within physical proximity.

---

### 5. Firmware & Hardware Security

#### 5.1 Boot Integrity
*Requirements for secure boot on hardware nodes using public-key checks of flash segments.*

#### 5.2 Storage Encryption
*Encryption of databases (SQLite/SQLCipher) stored on client devices.*

---

### 6. Security Guidelines for Contributors

* Never commit keys, private passwords, or certificates to code.
* Always check for buffer overflows when parsing binary protocols.
* Report vulnerabilities following the protocol defined in `SECURITY.md`.

---

## References

* *[Ref-01] Noise Protocol Framework Reference*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
