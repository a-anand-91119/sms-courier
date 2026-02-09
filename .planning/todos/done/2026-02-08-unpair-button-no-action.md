# UAT Issue: Unpair Button Does Nothing in Device History Bottom Sheet

**Created:** 2026-02-08
**Source:** Manual verification of v0.0.64
**Priority:** must
**Type:** Bug

## Problem

Clicking the Unpair button in the Device History bottom sheet has no effect.

## Steps to Reproduce

1. Open Device History
2. Tap on a paired device to open bottom sheet
3. Click Unpair button
4. Nothing happens (no confirmation dialog, no action)

## Expected Behavior

- Confirmation dialog should appear
- On confirm, device should be unpaired and archived
- Bottom sheet should close
- Device should move to "Removed Devices" section

## Affected Components

- `DeviceHistoryScreen.kt` — Bottom sheet unpair action
- `DeviceHistoryViewModel.kt` — Unpair handler

## Acceptance Criteria

- [ ] Unpair button shows confirmation dialog
- [ ] Confirming unpair removes device from active list
- [ ] Device appears in removed devices section
- [ ] Bottom sheet closes after action
