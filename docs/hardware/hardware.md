# Hardware Specification

## Document Metadata

* **Document ID:** `DOC-HW-001`
* **Version:** `0.1.0`
* **Status:** Draft
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Purpose & Scope

This document defines the hardware requirements, reference designs, and specifications for Project Beacon external radio nodes and dedicated hardware platforms.

---

## Design Principles

1. **Open Hardware**: All designs published under CERN-OHL-S-2.0 / CC-BY-SA-4.0
2. **Commodity Components**: Prefer widely available, non-obsolete parts
3. **Multi-Source**: No single-source critical components
4. **Field Repairable**: Through-hole where practical; modular design
5. **Regulatory Compliance**: FCC Part 15, CE RED, IC RSS-210
6. **Solar Ready**: Integrated solar charging capability

---

## Beacon Radio Node (Reference Design: BRN-1)

### System Block Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      BEACON RADIO NODE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Solar      │    │   Battery    │    │   Power      │      │
│  │   Panel      │───►│   (LiFePO4)  │───►│   Management │      │
│  │   (Optional) │    │   3.7V/5Ah   │    │   (BQ25570)  │      │
│  └──────────────┘    └──────────────┘    └──────┬───────┘      │
│                                                  │              │
│                                                  ▼              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   ESP32-S3   │◄───►│   LoRa       │    │   Antenna    │      │
│  │   (MCU)      │     │   (SX1262)   │    │   (915/868)  │      │
│  └──────┬───────┘     └──────────────┘    └──────────────┘      │
│         │                                                         │
│         │ USB-C / UART                                            │
│         ▼                                                         │
│  ┌──────────────┐                                                 │
│  │   Bluetooth  │                                                 │
│  │   (BLE 5.0)  │                                                 │
│  └──────────────┘                                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Core Specifications

| Parameter | Specification |
|-----------|---------------|
| **MCU** | ESP32-S3 (Xtensa LX7, 240 MHz, 512 KB SRAM, 8 MB PSRAM) |
| **LoRa Transceiver** | Semtech SX1262 (860-930 MHz, +22 dBm max) |
| **Bluetooth** | ESP32-S3 integrated (BLE 5.0, LE 2M PHY, Long Range) |
| **Battery** | LiFePO4 3.2V 5000 mAh (or 18650 holder) |
| **Solar Input** | 5V-6V, up to 2W (MPPT) |
| **USB** | USB-C (power + CDC/ACM serial) |
| **Enclosure** | IP67, UV-resistant, pole/wall mount |
| **Operating Temp** | -20°C to +60°C |
| **Dimensions** | 120×80×40 mm (PCB: 100×60 mm) |
| **Weight** | ~200g (with battery) |

### LoRa Radio Parameters

| Parameter | Value | Notes |
|-----------|-------|-------|
| **Frequency** | 915 MHz (US) / 868 MHz (EU) / 433 MHz (Asia) | Region-specific firmware |
| **Spreading Factor** | SF7-SF12 (configurable) | SF7=fastest, SF12=longest range |
| **Bandwidth** | 125 kHz (default), 250/500 kHz | |
| **Coding Rate** | 4/5, 4/6, 4/7, 4/8 | |
| **TX Power** | Up to +22 dBm (160 mW) | Regulatory limited |
| **RX Sensitivity** | -137 dBm (SF12, 125 kHz) | |
| **Max Range** | 5-15 km (rural), 1-3 km (urban) | Measured, not theoretical |

### Power Budget (Estimated)

| Mode | Current | Duration (5Ah) | Notes |
|------|---------|----------------|-------|
| **Deep Sleep** | 15 µA | 3.8 years | RTC only |
| **BLE Advertising** | 2 mA (avg) | 104 days | 500ms interval |
| **BLE Connected** | 8 mA (avg) | 26 days | 100ms interval |
| **LoRa RX** | 6 mA | 35 days | Continuous |
| **LoRa TX (+22 dBm)** | 120 mA | 41 hours | 1% duty cycle |
| **Active Mesh** | 25 mA (avg) | 8 days | Mixed TX/RX/BLE |

### Pinout (ESP32-S3 to SX1262)

| ESP32-S3 | SX1262 | Function |
|----------|--------|----------|
| GPIO 9 | NSS | SPI Chip Select |
| GPIO 10 | SCK | SPI Clock |
| GPIO 11 | MOSI | SPI Data Out |
| GPIO 12 | MISO | SPI Data In |
| GPIO 13 | BUSY | Busy Indicator |
| GPIO 14 | DIO1 | Interrupt (TX done, RX done) |
| GPIO 15 | RESET | Hardware Reset |
| GPIO 16 | DIO2 | RF Switch Control (optional) |
| GPIO 17 | DIO3 | TCXO Control (optional) |

### Firmware Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      FIRMWARE LAYERS                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    APPLICATION LAYER                       │   │
│  │  • Beacon Protocol Handler (CBOR/COBS)                   │   │
│  │  • Mesh Routing (Hybrid: Geographic + Epidemic)          │   │
│  │  • Power Management State Machine                        │   │
│  │  • BLE Peripheral (Phone Bridge)                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    TRANSPORT LAYER                         │   │
│  │  • LoRa Driver (SX126x HAL)                               │   │
│  │  • BLE Stack (NimBLE / ESP-IDF)                          │   │
│  │  • USB CDC/ACM (Serial Bridge)                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    OS / RTOS LAYER                         │   │
│  │  • Zephyr RTOS / ESP-IDF (FreeRTOS)                      │   │
│  │  • Flash filesystem (LittleFS)                           │   │
│  │  • Secure boot / OTA updates                             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Phone Bridge Protocol (Bluetooth Serial)

```
Phone ↔ Radio Node (Bluetooth Serial / BLE GATT)

Commands (ASCII, newline-terminated):
  AT+SEND=<hex_payload>          → Send Beacon frame
  AT+RECV                        ← Receive Beacon frame (async notification)
  AT+CONFIG=<key>=<value>        → Configure (freq, SF, power, etc.)
  AT+STATUS                      ← Status JSON (battery, RSSI, peers, etc.)
  AT+RESET                       → Soft reset
  AT+DFU                         → Enter DFU mode

Responses:
  OK
  ERROR=<code>
  +EVT:RECV=<hex_payload>
  +EVT:PEER=<peer_json>
  +EVT:STATS=<stats_json>
```

### Enclosure Design

| Requirement | Specification |
|-------------|---------------|
| **Material** | ASA (UV-resistant) or Polycarbonate |
| **IP Rating** | IP67 (1m, 30 min) |
| **Mounting** | Pole clamp (1-3"), wall screws, magnetic base |
| **Antenna** | Internal PCB antenna + external SMA option |
| **Indicators** | RGB LED (status), visible through diffuser |
| **Buttons** | Reset (recessed), Pairing (accessible) |
| **Ventilation** | Gore-Tex vent (pressure equalization) |

---

## Beacon Gateway (Reference Design: BGW-1)

### Purpose

Fixed infrastructure node for:
- Community mesh backbone
- Internet gateway (when available)
- High-power LoRa coverage
- Dashboard hosting

### Specifications

| Parameter | Specification |
|-----------|---------------|
| **SBC** | Raspberry Pi 4 CM4 / Radxa CM3 / Orange Pi 5 |
| **LoRa Concentrator** | Semtech SX1302 / SX1303 (8 channels) |
| **BLE** | SBC integrated + external nRF52840 dongle |
| **Wi-Fi** | SBC integrated (AP + STA) |
| **Ethernet** | 1 Gbps |
| **Storage** | 32 GB eMMC + SSD (optional) |
| **Power** | 12V DC (PoE+ optional), solar ready |
| **OS** | Beacon OS (Yocto-based) |
| **Enclosure** | IP66, DIN rail or pole mount |

---

## Antenna Design Guidelines

### LoRa Antenna (915/868 MHz)

| Type | Gain | Pattern | Use Case |
|------|------|---------|----------|
| **PCB Meander** | 0-1 dBi | Omnidirectional | Integrated, low cost |
| **Whip (1/4 λ)** | 2-3 dBi | Omnidirectional | External, best range |
| **Yagi (3-6 el)** | 6-9 dBi | Directional | Point-to-point links |
| **Patch** | 4-6 dBi | Directional | Sector coverage |

### BLE Antenna (2.4 GHz)

- Use ESP32-S3 internal antenna (meander on PCB)
- Keep clear area: 10mm radius, no metal
- Optional: U.FL connector for external 2.4 GHz antenna

---

## Regulatory Compliance

| Region | Standard | Frequency | Max Power | Duty Cycle |
|--------|----------|-----------|-----------|------------|
| **USA** | FCC Part 15.247 | 902-928 MHz | +30 dBm (1W) | No limit (spread spectrum) |
| **Canada** | RSS-210 | 902-928 MHz | +30 dBm | No limit |
| **Europe** | ETSI EN 300 220 | 863-870 MHz | +14 dBm (25 mW) | 1% / 10% (sub-bands) |
| **Australia** | AS/NZS 4268 | 915-928 MHz | +30 dBm | No limit |
| **Japan** | ARIB STD-T108 | 920-925 MHz | +13 dBm | 1% |

**Note**: Firmware must enforce regional limits. Region set at manufacturing or via secure config.

---

## Bill of Materials (BRN-1, Estimated)

| Component | Qty | Unit Cost | Total | Notes |
|-----------|-----|-----------|-------|-------|
| ESP32-S3-WROOM-1 | 1 | $3.50 | $3.50 | 8MB PSRAM |
| SX1262 | 1 | $2.80 | $2.80 | LoRa transceiver |
| BQ25570 | 1 | $1.20 | $1.20 | Solar charger |
| LiFePO4 3.2V 5Ah | 1 | $8.00 | $8.00 | Or 18650 holder |
| USB-C connector | 1 | $0.30 | $0.30 | 16-pin, USB 2.0 |
| PCB (4-layer) | 1 | $4.00 | $4.00 | 100×60mm, ENIG |
| SMA connector | 1 | $0.50 | $0.50 | Optional external ant |
| Enclosure (ASA) | 1 | $6.00 | $6.00 | Injection molded |
| **Total** | | | **~$26.30** | Volume pricing lower |

---

## Manufacturing & Assembly

| Step | Specification |
|------|---------------|
| **PCB** | 4-layer, 1.6mm, ENIG, 0.15mm min trace/space |
| **Assembly** | SMT (reflow) + hand solder (connectors, battery) |
| **Test** | ICT + functional (LoRa TX/RX, BLE, USB, Solar) |
| **Programming** | JTAG (ESP-Prog) / USB DFU |
| **Calibration** | TX power, frequency offset, crystal trim |

---

## Future Hardware Variants

| Variant | Purpose | Key Changes |
|---------|---------|-------------|
| **BRN-1-Lite** | Low-cost node | ESP32-C3, no solar, coin cell |
| **BRN-1-Pro** | Extended range | SX1280 (2.4 GHz LoRa), PA/LNA |
| **BRN-1-Sat** | Satellite backup | Swarm/Globalstar modem addon |
| **Badge** | Wearable | nRF52840, e-ink display, BLE only |

---

## References

* [Architecture Overview](../architecture/architecture.md)
* [ADR-0001: BLE Discovery](../adr/ADR-0001-ble-discovery.md)
* Semtech SX1262 Datasheet
* ESP32-S3 Technical Reference Manual
* FCC Part 15.247 / ETSI EN 300 220

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 0.1.0 | Initial draft | Project Beacon Core Team |