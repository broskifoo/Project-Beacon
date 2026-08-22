# UI/UX Specification Template

## Document Metadata

* **Document ID:** `DOC-UIUX-001`
* **Version:** `0.1.0`
* **Status:** Draft / Proposed
* **Author:** *Placeholder Author*
* **Reviewers:** *Placeholder Reviewer 1, Placeholder Reviewer 2*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
This UI/UX Specification outlines design systems, visual styles, navigation architecture, and accessibility standards for the user-facing interfaces of Project Beacon. It ensures consistency across platforms and priority-based readability under extreme physical stress.

### Scope
This spec covers styling, color theories, typography, layout guidelines, asset files, and user workflows for the mobile client and `beacon-dashboard`. Hardware displays (LED indicators, local e-ink screen layouts) are also governed by these high-level principles.

---

## Table of Contents

1. [Design Philosophy & Core Pillars](#1-design-philosophy--core-pillars)
2. [Color Systems & High Contrast](#2-color-systems--high-contrast)
3. [Typography & Readability](#3-typography--readability)
4. [Navigation & Information Architecture](#4-navigation--information-architecture)
5. [Key Component Design Guidelines](#5-key-component-design-guidelines)
6. [Accessibility (WCAG) & Extreme Conditions](#6-accessibility-wcag--extreme-conditions)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Design Philosophy & Core Pillars

* **Clarity Over Style:** In emergency situations, interface visual fluff must be avoided. Clean contrast and obvious button states are paramount.
* **Offline Indicator First:** The connection status of the local hardware node must be clearly visible on every screen.
* **Low Power Optimizations:** Default dark mode theme to conserve AMOLED screen batteries.

---

### 2. Color Systems & High Contrast

To support readable screens under direct sunlight or smoke, we use high-contrast color pairings:

| Element | Dark Mode Hex | Light Mode Hex | Purpose |
|---|---|---|---|
| **Background** | `#121212` (OLED black) | `#FFFFFF` | Core Canvas |
| **SOS Action** | `#E53935` (Crimson) | `#D32F2F` | Emergency Buttons / Alerts |
| **Success/Connected**| `#4CAF50` (Green) | `#388E3C` | Node Connected States |
| **Text Primary** | `#FFFFFF` (High opacity) | `#212121` | Information Readout |

---

### 3. Typography & Readability

* **Primary Font:** Sans-serif typefaces with high legibility at small sizes (e.g., *Inter*, *Outfit*, or *Roboto*).
* **Font Scaling:**
  * Header 1: 24sp (Bold)
  * Subheader: 18sp (Semi-Bold)
  * Body Text: 14sp-16sp (Regular)
  * Status/Metadata: 12sp (Monospace, clear numbers)

---

### 4. Navigation & Information Architecture

#### 4.1 Mobile Navigation Layout (Bottom Bar)
* **Tab 1: Map Dashboard** - Show geographic nodes and user location.
* **Tab 2: Messages** - Chat channels and broadcast lists.
* **Tab 3: Hardware Diagnostics** - RF status, battery health, and BLE link stats.
* **Tab 4: Settings** - Local encryption keys and node configuration.

---

### 5. Key Component Design Guidelines

#### 5.1 SOS Distress Button
* *Design:* Red circular button, minimum touch target area `80dp x 80dp`.
* *Behavior:* Requires a 3-second continuous long press to activate, providing visual progress feedback to avoid accidental triggers.

---

### 6. Accessibility (WCAG) & Extreme Conditions

* **Contrast Ratio:** Minimum contrast ratio of `7:1` for all critical text and icons.
* **Screen Reader Compatibility:** Proper label structures for screen readers on mobile client software.
* **Tactile Feedback:** Haptic patterns (vibrations) on trigger confirmations.

---

## References

* *[Ref-01] Google Material Design Guidelines*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 0.1.0 | Initial template layout. | Antigravity |
