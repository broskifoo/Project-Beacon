# Meeting Notes Template

## Document Metadata

* **Document ID:** `DOC-MEET-YYYY-MM-DD`
* **Version:** `1.0.0`
* **Status:** Final
* **Author:** *Placeholder Scribe*
* **Reviewers:** *All Attendees*
* **Last Updated:** *Placeholder Date*

---

## Purpose & Scope

### Purpose
The Meeting Notes capture discussions, design decisions, architectural consensus, and action items originating from sync calls, design reviews, or standup meetings.

### Scope
This template applies to all official Project Beacon meetings including architectural reviews, steering committee meetings, and sprint planning sessions.

---

## Table of Contents

1. [Meeting Details](#1-meeting-details)
2. [Meeting Agenda](#2-meeting-agenda)
3. [Discussion Summary](#3-discussion-summary)
4. [Key Decisions Made](#4-key-decisions-made)
5. [Action Items Matrix](#5-action-items-matrix)
6. [References](#references)
7. [Revision History](#revision-history)

---

## Main Sections

### 1. Meeting Details

* **Date:** *Placeholder: YYYY-MM-DD*
* **Time:** *Placeholder: HH:MM UTC*
* **Location / Link:** *Placeholder: Jitsi/Discord/Google Meet Link*
* **Facilitator:** *Placeholder Name*
* **Attendees:**
  * *Attendee 1 Name (Affiliation)*
  * *Attendee 2 Name (Affiliation)*
  * *Attendee 3 Name (Affiliation)*

---

### 2. Meeting Agenda

1. *Topic 1: Review of Milestone 1 progress.*
2. *Topic 2: Discussion on `beacon-mesh` serialization strategy.*
3. *Topic 3: Feedback on hardware enclosure designs.*

---

### 3. Discussion Summary

#### 3.1 Topic 1: Milestone 1 Progress Review
*Summary of details discussed during the sync. Note major opinions, suggestions, and technical considerations raised.*

#### 3.2 Topic 2: Mesh Serialization Format
*Summary of debate regarding Protocol Buffers vs FlatBuffers for low-power microcontrollers.*

---

### 4. Key Decisions Made

* **Decision-01:** Standardize on Protocol Buffers (v3) for L7 payloads due to mature tooling across Swift, Kotlin, and C++.
* **Decision-02:** Limit packet MTU to 256 bytes for radio compatibility.

---

### 5. Action Items Matrix

| Task Description | Assigned Owner | Priority | Target Due Date | Status |
|---|---|---|---|---|
| Draft protocol buffer schema for SOS payload | *Owner Name* | High | YYYY-MM-DD | Not Started |
| Test SPI frequency limitations on SX1262 | *Owner Name* | Medium | YYYY-MM-DD | In Progress |

---

## References

* *[Ref-01] Link to presentation slide decks or board boards.*

---

## Revision History

| Date | Version | Description | Author |
|---|---|---|---|
| YYYY-MM-DD | 1.0.0 | Initial template layout. | Antigravity |
