# Protocol Specification

## Document Metadata

* **Document ID:** `DOC-PROTOCOL-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the wire protocol for Project Beacon mesh communications. It covers packet formats, message types, routing headers, encryption, and transport-specific framing.

---

## Packet Structure

### Beacon Frame (Transport-Agnostic)

```
┌─────────────────────────────────────────────────────────────────┐
│                        BEACON FRAME                              │
├──────────┬──────────┬──────────┬──────────┬──────────────────────┤
│  Magic   │ Version  │  Flags   │  Type    │      Payload         │
│  (2 B)   │  (1 B)   │  (1 B)   │  (1 B)   │      (Variable)      │
├──────────┴──────────┴──────────┴──────────┴──────────────────────┤
│                    Authentication Tag (16 B)                       │
└─────────────────────────────────────────────────────────────────┘
```

| Field | Size | Description |
|-------|------|-------------|
| Magic | 2 bytes | `0xBEAC` - Protocol identifier |
| Version | 1 byte | Protocol version (currently `0x01`) |
| Flags | 1 byte | Bitfield (see below) |
| Type | 1 byte | Message type (see Message Types) |
| Payload | Variable | Encrypted message payload |
| Auth Tag | 16 bytes | ChaCha20-Poly1305 authentication tag |

### Flags Bitfield

| Bit | Name | Description |
|-----|------|-------------|
| 0 | `ENCRYPTED` | Payload is encrypted (always 1 for v1) |
| 1 | `SIGNED` | Payload includes Ed25519 signature |
| 2 | `FRAGMENTED` | This is a fragment of a larger message |
| 3 | `ACK_REQUESTED` | Sender requests acknowledgment |
| 4 | `PRIORITY` | High priority (1) / Normal (0) |
| 5 | `RELAY` | This frame is being relayed (not original) |
| 6-7 | Reserved | Must be 0 |

---

## Routing Header (Prepended by Mesh Layer)

```
┌─────────────────────────────────────────────────────────────────┐
│                       ROUTING HEADER                             │
├────────────┬────────────┬────────┬────────┬────────┬────────────┤
│  Dest ID   │  Source ID │  TTL   │ Hops   │ Msg ID │  Reserved  │
│  (32 B)    │  (32 B)    │ (1 B)  │ (1 B)  │ (16 B) │   (2 B)    │
└────────────┴────────────┴────────┴────────┴────────┴────────────┘
```

| Field | Size | Description |
|-------|------|-------------|
| Dest ID | 32 bytes | Destination peer Ed25519 public key (or 32 zero bytes for broadcast) |
| Source ID | 32 bytes | Source peer Ed25519 public key |
| TTL | 1 byte | Time-to-live in hops (default 5, max 255) |
| Hops | 1 byte | Number of hops traversed |
| Msg ID | 16 bytes | Unique message identifier (UUIDv7) |
| Reserved | 2 bytes | Must be 0 |

---

## Message Payload (Application Layer)

After decryption, the payload contains:

```
┌─────────────────────────────────────────────────────────────────┐
│                      MESSAGE PAYLOAD                             │
├──────────┬────────────┬────────────┬────────────────────────────┤
│ Msg Type │ Timestamp  │  Priority  │      Content (CBOR)        │
│  (1 B)   │  (8 B)     │   (1 B)    │      (Variable)            │
└──────────┴────────────┴────────────┴────────────────────────────┘
```

### Message Types

| Value | Type | Description |
|-------|------|-------------|
| 0x01 | TEXT | Plain text message |
| 0x02 | LOCATION | GPS coordinates |
| 0x03 | TELEMETRY | Battery, signal, sensor data |
| 0x04 | SOS | Emergency distress signal |
| 0x05 | ACKNOWLEDGMENT | Delivery confirmation |
| 0x06 | RESOURCE_REPORT | Community resource report |
| 0x07 | ALERT | Broadcast alert |
| 0x08 | MAP_TILE | Map tile data fragment |
| 0x09 | VOICE_NOTE | Compressed audio |
| 0x0A | IMAGE | Compressed image |
| 0x0B | KEY_EXCHANGE | Ephemeral key agreement |
| 0x0C | PEER_DISCOVERY | Neighbor announcement |
| 0x0D | ROUTING_UPDATE | Mesh topology update |
| 0xFF | CUSTOM | Application-defined |

### Priority Values

| Value | Priority | Description |
|-------|----------|-------------|
| 0x00 | LOW | Background, non-critical |
| 0x01 | NORMAL | Standard messages |
| 0x02 | HIGH | Important, time-sensitive |
| 0x03 | CRITICAL | Emergency, life-safety |

---

## Cryptography

### Key Derivation

```
Identity Key (Ed25519) → Long-term identity, stored in secure enclave
  │
  ├─→ Signing Key (Ed25519) → Message signatures
  │
  └─→ Key Agreement (X25519) → ECDH for session keys
        │
        └─→ Session Key (ChaCha20-Poly1305) → Message encryption
              Derived via HKDF-SHA256(shared_secret, salt="beacon-v1", info=session_context)
```

### Encryption Format (ChaCha20-Poly1305)

```
Nonce (12 bytes): [Message ID (8 bytes) || Counter (4 bytes)]
AAD: Routing Header (84 bytes)
Plaintext: Message Payload
Ciphertext: Encrypted payload + 16-byte auth tag
```

### Signature Format (Ed25519)

```
Signed Data: Routing Header || Ciphertext
Signature: 64 bytes (appended to frame when FLAGS.SIGNED=1)
```

---

## Transport-Specific Framing

### BLE (L2CAP CoC)

```
┌─────────────────────────────────────────────────────────────────┐
│                        BLE FRAME                                 │
├────────────┬────────────────────────────────────────────────────┤
│  PSM       │              Beacon Frame                           │
│ (0xBEAC)   │  (MTU up to 65535, negotiated)                      │
└────────────┴────────────────────────────────────────────────────┘
```

- PSM: `0xBEAC` (registered)
- MTU: Negotiated up to 65535 bytes
- Flow control: L2CAP credit-based

### Wi-Fi Direct

```
┌─────────────────────────────────────────────────────────────────┐
│                      WIFI-DIRECT FRAME                           │
├────────────┬────────────┬───────────────────────────────────────┤
│  EtherType │  Length    │           Beacon Frame                │
│  (0xBEAC)  │  (2 B)     │         (up to 64 KB)                 │
└────────────┴────────────┴───────────────────────────────────────┘
```

- Custom EtherType: `0xBEAC`
- Transport: TCP over Wi-Fi P2P group
- Port: 4242 (unregistered, ephemeral)

### LoRa (External Radio)

```
┌─────────────────────────────────────────────────────────────────┐
│                         LORA FRAME                               │
├──────────┬──────────┬──────────┬────────────────────────────────┤
│ Preamble │  Sync    │  Beacon    │       CRC (2 B)              │
│  (8 B)   │ Word     │  Frame     │                               │
│          │ (4 B)    │ (Var, max  │                               │
│          │          │  255 B)    │                               │
└──────────┴──────────┴────────────┴────────────────────────────┘
```

- Modulation: LoRa (SF7-SF12, configurable)
- Bandwidth: 125/250/500 kHz
- Coding Rate: 4/5, 4/6, 4/7, 4/8
- Max payload: 255 bytes (fragmented for larger messages)

---

## Fragmentation & Reassembly

For messages exceeding transport MTU:

```
Fragment Header (prepended to each fragment):
┌──────────┬──────────┬──────────┬──────────┐
│  Msg ID  │ Frag Idx │ Frag Tot │ Frag Len │
│  (16 B)  │  (2 B)   │  (2 B)   │  (2 B)   │
└──────────┴──────────┴──────────┴──────────┘
```

- Fragments sent sequentially with small delay
- Reassembly timeout: 30 seconds
- Missing fragments → entire message dropped

---

## Handshake Protocol

### 1. Discovery (BLE Advertising)

```
Advertising Data (31 bytes):
┌────────────┬────────────┬────────────┬────────────┐
│ Flags (3)  │ UUID (17)  │ DevHash(8) │ Bat/Mode(3)│
└────────────┴────────────┴────────────┴────────────┘
```

### 2. Connection & Key Exchange

```
Initiator → Responder: KEY_EXCHANGE
  Ephemeral Public Key (X25519, 32 bytes)
  Identity Public Key (Ed25519, 32 bytes)
  Signature over both keys (Ed25519, 64 bytes)
  Timestamp (8 bytes)

Responder → Initiator: KEY_EXCHANGE
  Ephemeral Public Key (X25519, 32 bytes)
  Identity Public Key (Ed25519, 32 bytes)
  Signature over both keys (Ed25519, 64 bytes)
  Timestamp (8 bytes)
```

### 3. Session Establishment

Both parties compute:
```
Shared Secret = X25519(our_ephemeral_priv, peer_ephemeral_pub)
Session Key = HKDF-SHA256(Shared Secret, salt="beacon-v1", info=session_context)
```

Session Context = `initiator_id || responder_id || timestamp`

---

## Mesh Routing Extensions

### Bundle Protocol (DTN)

For store-and-forward across intermittent connectivity:

```
Bundle Header:
┌────────────┬────────────┬────────┬────────┬────────┬────────────┤
│  Dest EID  │ Source EID │ Lifetime │ Flags  │ Seq No │  Blocks  │
│  (Var)     │  (Var)     │ (4 B)    │ (1 B)  │ (4 B)  │  (Var)   │
└────────────┴────────────┴────────┴────────┴────────┴────────────┘
```

- EID: Endpoint ID = `dtn://<peer_id>/<service>`
- Lifetime: Bundle expiration (seconds from creation)
- Blocks: Payload, metadata, extension blocks

---

## Error Codes

| Code | Name | Description |
|------|------|-------------|
| 0x00 | SUCCESS | Operation completed |
| 0x01 | ERR_INVALID_FRAME | Malformed frame |
| 0x02 | ERR_DECRYPT_FAILED | AEAD decryption failed |
| 0x03 | ERR_SIG_VERIFY_FAILED | Signature verification failed |
| 0x04 | ERR_TTL_EXPIRED | TTL reached 0 |
| 0x05 | ERR_DUPLICATE | Duplicate message ID |
| 0x06 | ERR_BUFFER_FULL | No buffer space |
| 0x07 | ERR_PEER_UNKNOWN | Destination not in neighbor table |
| 0x08 | ERR_TRANSPORT_DOWN | No active transport |
| 0x09 | ERR_FRAGMENT_TIMEOUT | Reassembly timeout |
| 0x0A | ERR_VERSION_MISMATCH | Unsupported protocol version |

---

## References

* [Architecture Overview](../architecture/architecture.md)
* [Security Specification](../security/security.md)
* [ADR-0001: BLE Discovery](../adr/ADR-0001-ble-discovery.md)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |