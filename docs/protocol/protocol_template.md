# Communication Protocol Specification Template

## Document Metadata

* **Document ID:** `DOC-PROT-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
This protocol specification defines the exact wire format, serialization structure, routing frames, and exchange sequences for Project Beacon nodes. It ensures interoperability between different hardware nodes and software versions.

### Scope
This spec covers Layer 2 (Link Framing), Layer 3 (Mesh Routing Header), Layer 4 (Transport / Session), and Layer 7 (Application Payload Types) of the Project Beacon protocol suite. It is binding for all `beacon-mesh` and `beacon-radio` implementations.

---

## Table of Contents

1. [Protocol Layers Overview](#1-protocol-layers-overview)
2. [Addressing & Identification](#2-addressing--identification)
3. [Packet Frame Formats](#3-packet-frame-formats)
4. [Routing Protocol Operations](#4-routing-protocol-operations)
5. [Serialization Formats](#5-serialization-formats)
6. [References](#references)
7. [Revision History](#revision-history)

---

## Main Sections

### 1. Protocol Layers Overview

| OSI Layer | Protocol / Abstract Name | Primary Function |
|---|---|---|
| **L7 Application** | Beacon Payloads | User messages, SOS coordinates, node status updates. |
| **L4 Transport** | Reliable Transport / UDP-like | End-to-end reliability, chunking, and deduplication. |
| **L3 Network** | Beacon Mesh Routing | Multi-hop routing tables, next-hop decision logic. |
| **L2 Link** | Radio Framing | Preamble, CRC, packet delimiting, link quality (RSSI). |
| **L1 Physical** | LoRa / FSK / BLE Physical | Modulation, channel configuration, power levels. |

---

### 2. Addressing & Identification

* **Node Address:** *Placeholder: 64-bit unique cryptographic address derived from public key.*
* **Network ID:** *Placeholder: 16-bit subnet identifier.*
* **Broadcast Address:** `0xFFFFFFFFFFFFFFFF` for target networks.

---

### 3. Packet Frame Formats

#### 3.1 Global Packet Structure (Binary Wire Layout)

```text
+-------------------+-------------------+-------------------+-------------------+
|  Magic Byte (8b)  |  Version (8b)     |  Flags (8b)       |  TTL / Hop (8b)  |
+-------------------+-------------------+-------------------+-------------------+
|                     Sender Cryptographic Address (64-bit)                     |
+-------------------------------------------------------------------------------+
|                    Recipient Cryptographic Address (64-bit)                   |
+-------------------------------------------------------------------------------+
|  Payload Type(8b) |  Payload Len(16b) |  Payload Data (Variable) ...          |
+-------------------+-------------------+---------------------------------------+
|                     Cryptographic Signature (256-bit)                         |
+-------------------------------------------------------------------------------+
```

* **Magic Byte:** Identifies valid Beacon packets.
* **Flags:** Bitmask for Encryption, Reliable Transport, Priority level.
* **TTL:** Time-To-Live hop counter (decremented each hop).

---

### 4. Routing Protocol Operations

#### 4.1 Route Discovery (Discovery Broadcast)
*Describe how discovery frames are sent, received, and processed to construct paths.*

#### 4.2 Link Metrics
*Details of link-quality scoring (e.g. RSSI, SNR, Packet Loss Ratio) and path cost calculation.*

---

### 5. Serialization Formats

* **Protobuf Definitions:**
```protobuf
// Protocol Buffer structure for L7 Payload (Placeholder)
syntax = "proto3";
package beacon.protocol;

message EmergencyBeacon {
  string sender_name = 1;
  double latitude = 2;
  double longitude = 3;
  uint64 timestamp = 4;
  enum Severity {
    LOW = 0;
    MEDIUM = 1;
    HIGH = 2;
    CRITICAL = 3;
  }
  Severity severity = 5;
}
```

---

## References

* *[Ref-01] Protocol Serialization Standard References*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
