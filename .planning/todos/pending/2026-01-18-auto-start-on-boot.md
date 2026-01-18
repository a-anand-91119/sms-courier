---
created: 2026-01-18T20:00
title: Auto-start app on device boot
area: feature-backlog
files: []
priority: backlog
---

## Problem

Users may want the app to automatically start when their device boots, ensuring SMS forwarding is always available without manual intervention.

## Solution

Deferred from v0.0.63 Settings due to complexity:

1. Add RECEIVE_BOOT_COMPLETED permission
2. Create BootCompletedReceiver broadcast receiver
3. Handle vendor-specific auto-start restrictions:
   - Xiaomi: MIUI battery saver whitelist
   - Huawei: EMUI power-intensive app management
   - Samsung: Device care battery optimization
4. Handle Android 12+ background start restrictions (WorkManager integration)
5. Add setting toggle to enable/disable auto-start
6. Guide users through vendor-specific settings when needed

Research notes in `.planning/research/PITFALLS.md` document the vendor quirks.
