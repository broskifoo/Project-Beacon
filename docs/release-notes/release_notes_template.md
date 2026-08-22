# Release Notes Template

## Document Metadata

* **Document ID:** `DOC-REL-vX.Y.Z`
* **Version:** `1.0.0`
* **Status:** Draft / Final
* **Author:** *Release Manager / Maintainer*
* **Reviewers:** *Core Maintainer Group*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
The Release Notes document the user-facing and developer-facing updates, enhancements, bug resolutions, and system changes associated with a official versioned release of the Project Beacon platform.

### Scope
This report addresses changes across all software, firmware, and SDK packages bundled in the release. It includes links to migration guides and credits contributing community members.

---

## Table of Contents

1. [Release Summary](#1-release-summary)
2. [What's New (Feature Highlights)](#2-whats-new-feature-highlights)
3. [Bug Fixes & Security Updates](#3-bug-fixes--security-updates)
4. [Breaking Changes & Deprecations](#4-breaking-changes--deprecations)
5. [Upgrade & Migration Instructions](#5-upgrade--migration-instructions)
6. [Contributors & Acknowledgments](#6-contributors--acknowledgments)
7. [References](#references)
8. [Revision History](#revision-history)

---

## Main Sections

### 1. Release Summary

* **Release Tag:** `vX.Y.Z` (e.g. `v1.0.0`)
* **Release Date:** *Placeholder: YYYY-MM-DD*
* **Release Type:** Major / Minor / Patch / Security Patch

*Provide a brief introductory paragraph characterizing the theme of this release (e.g., "This release introduces our first production-ready mesh routing stack...").*

---

### 2. What's New (Feature Highlights)

*Detailed explanation of new capabilities.*

* **Feature Title:** *e.g., Dynamic Route Recovery*
  * *Description:* When a next-hop link drops, the node now automatically probes alternative routes.
  * *PR Reference:* PR #105.

---

### 3. Bug Fixes & Security Updates

*List of resolved bugs and vulnerabilities.*

* **Fix 1:** Resolved buffer overflow when receiving malformed BLE configuration frames (CVE-XXXX-XXXX).
* **Fix 2:** Corrected GPS longitude encoding bug in SOS packet creation.

---

### 4. Breaking Changes & Deprecations

*Critical notices regarding API or configuration changes that require manual code updates.*

* **Breaking Change:** The configuration interface `RadioConfig` field `tx_power` has been renamed to `transmit_power_dbm`.
* **Deprecation:** The legacy `on_node_link_down` callback signature is deprecated and will be removed in `v2.0.0`.

---

### 5. Upgrade & Migration Instructions

*Instructions on how to update client code or settings.*

```bash
# Example update command (Placeholder)
npm install @project-beacon/sdk@latest
# or
git pull upstream main
```

*Describe manual steps to upgrade database schemas if applicable.*

---

### 6. Contributors & Acknowledgments

We would like to thank the following community contributors for their commits, reviews, and bug reports in this release:

* *@github_username1* (For implementation of routing tables)
* *@github_username2* (For bug reports in the UI client)

---

## References

* *[Ref-01] Upgrade Migration Guide URL*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 1.0.0 | Initial template layout. | Antigravity |
