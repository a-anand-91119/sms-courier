---
created: 2026-02-16T11:00
title: Device History cards need three-row layout on small screens
area: ui-polish
files:
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/DeviceHistoryScreen.kt
priority: backlog
---

## Problem

On small screen devices (S22, S24 ~360dp), Device History device cards display "last active" text vertically/cramped. The current layout tries to fit too much into one or two rows.

## Solution

Restructure device cards for small screens into a three-row layout:
1. **Row 1:** Phone number + SOURCE/TARGET badge
2. **Row 2:** Last active timestamp
3. **Row 3:** Session count + message count

Could use BoxWithConstraints similar to DirectionalStatusCard (400dp breakpoint pattern established in Phase 25-02).

## Context

Discovered during Phase 25 HOME-02 verification on Samsung S24. The DirectionalStatusCard compact layout was built for the Home screen, but Device History cards have the same small-screen issue.
