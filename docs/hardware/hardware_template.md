# Hardware Specification Template

## Document Metadata

* **Document ID:** `DOC-HW-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
The Hardware Specification describes the physical, electrical, mechanical, and RF design requirements for Project Beacon radio nodes. It serves as a guide for circuit designers, enclosure engineers, and firmware developers.

### Scope
This spec details target microcontrollers, RF modules, power distribution modules, antenna parameters, and enclosure metrics. Software integrations and firmware logic details are out of scope (covered in SRS and `beacon-radio` specs).

---

## Table of Contents

1. [Hardware Architecture Overview](#1-hardware-architecture-overview)
2. [Microcontroller Unit (MCU) Specifications](#2-microcontroller-unit-mcu-specifications)
3. [RF & Transceiver Subsystems](#3-rf--transceiver-subsystems)
4. [Power Management & Solar Charger](#4-power-management--solar-charger)
5. [Pinout & Interface Map](#5-pinout--interface-map)
6. [Enclosure & Mechanical Specs](#6-enclosure--mechanical-specs)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Hardware Architecture Overview

```text
+-----------------------------------------------------------+
|                        Beacon Node                        |
|                                                           |
|  +--------------+       +-------------+    +-----------+  |
|  |  Solar Panel | ----> | Power Mgmt  | -> | LiFePO4   |  |
|  +--------------+       | (MPPT IC)   |    | Battery   |  |
|                         +-------------+    +-----------+  |
|                                |                          |
|                                v                          |
|  +--------------+       +-------------+    +-----------+  |
|  | GPS Module   | <---> | Primary MCU | <- | BLE Chip  |  |
|  +--------------+       | (STM32/ESP) |    +-----------+  |
|                         +-------------+                   |
|                                ^                          |
|                                v                          |
|                         +-------------+    +-----------+  |
|                         | LoRa Chip   | -> | Antenna   |  |
|                         | (SX1262)    |    | (SMA Connector)
|                         +-------------+    +-----------+  |
+-----------------------------------------------------------+
```

---

### 2. Microcontroller Unit (MCU) Specifications

* **Primary MCU:** *Placeholder: Dual-core ARM Cortex-M4 or ESP32-S3.*
* **Memory Limits:** *Placeholder: Min 512KB SRAM, 4MB Flash.*
* **Operating Voltage:** 3.3V DC logic.

---

### 3. RF & Transceiver Subsystems

* **Primary RF IC:** SX1262 LoRa Transceiver or similar.
* **Frequency Bands:**
  * Region 1 (Americas): 915 MHz ISM band.
  * Region 2 (Europe): 868 MHz ISM band.
  * Region 3 (Asia): 433 MHz / 868 MHz.
* **TX Power Level:** Up to +22 dBm adjustable.
* **Receiver Sensitivity:** -148 dBm.

---

### 4. Power Management & Solar Charger

* **Battery Type:** LiFePO4 (Lithium Iron Phosphate) for high thermal stability and cycle life.
* **Charging Interface:** Integrated MPPT (Maximum Power Point Tracking) charger for 5V-12V solar input panels.
* **Power Modes:** Deep Sleep state current under 50uA.

---

### 5. Pinout & Interface Map

| Port / Interface | Connection Partner | Protocol | Speed / Parameters |
|---|---|---|---|
| `UART1` | GPS module | UART | 9600 bps, 8N1 |
| `SPI1` | SX1262 Transceiver | SPI | 10 MHz |
| `I2C1` | Environmental Sensor | I2C | 400 kHz |

---

### 6. Enclosure & Mechanical Specs

* **Ingress Protection:** IP67 water and dust resistant casing.
* **Mounting:** Pole-mount brackets and tie-wrap slots.
* **Operating Temperature:** -40°C to +85°C.

---

## References

* *[Ref-01] SX1262 Chipset Datasheet Reference*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
