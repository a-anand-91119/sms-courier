---
created: 2026-02-09T18:31
title: "Orphan sessions visible after re-pairing device"
area: uat
priority: high
files:
  - app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt
---

## Problem

When adding a device that existed previously (re-pairing), old sessions are visible but:
1. Sessions show 0 messages
2. No way to clean up these orphan sessions
3. Settings cleanup reports "no active sessions" even when orphan sessions exist

## Solution

TBD - May be related to session tracking bug. Investigate session lifecycle during device unarchive/re-pair flow.
