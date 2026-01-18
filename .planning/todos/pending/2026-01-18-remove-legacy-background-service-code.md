---
created: 2026-01-18T20:00
title: Remove legacy unused background service code
area: tech-debt
files: [app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt]
priority: low
---

## Problem

During settings milestone planning, we discovered unused/vestigial code in MasterService related to a background service pattern that was never fully implemented:

- `stopDelay` constant (5 minutes) at line 49
- `startBackgroundService()` function at line 479
- `stopBackgroundService()` function at line 469
- `START_BACKGROUND` action constant at line 87
- `STOP_BACKGROUND` action constant

The `START_BACKGROUND` action is defined but never triggered anywhere in the app. This code path is dead.

## Solution

Remove the unused background service code:
1. Delete `stopDelay` constant
2. Delete `startBackgroundService()` function
3. Delete `stopBackgroundService()` function
4. Delete `START_BACKGROUND` and `STOP_BACKGROUND` action constants
5. Remove the corresponding case handlers in `onStartCommand()`

This cleanup reduces code complexity and removes confusion about the service architecture.
