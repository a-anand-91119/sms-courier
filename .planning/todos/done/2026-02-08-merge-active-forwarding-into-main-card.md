# UAT Issue: Merge Active Forwarding into Main Card

**Created:** 2026-02-08
**Source:** Manual verification of v0.0.64
**Priority:** should
**Type:** UI consolidation

## Problem

The home screen currently shows active forwarding information in **two places**:

1. **Main status card** — Has tabs/sections for Paired, Pending, and Active
2. **Active Forwarding section** — Separate section below the main card

This is redundant and confusing. Users see the same active forwarding state represented twice.

## Expected Behavior

The "Active" tab/section in the main status card should contain all active forwarding information, including:
- Directional indicators (↑ forwarding to, ↓ receiving from, ⇅ bidirectional)
- Active session count
- Device details for active sessions
- Ability to tap for session breakdown

The separate "Active Forwarding" section below should be removed.

## Affected Components

- `HomeScreen.kt` — Layout structure
- `DirectionalStatusCard.kt` — May need integration into main card
- `HomeViewModel.kt` — State consolidation (if needed)

## Acceptance Criteria

- [ ] Active forwarding info displayed only in main card's Active section
- [ ] No duplicate active forwarding section below
- [ ] Directional indicators (↑↓⇅) visible in Active section
- [ ] Tap behavior opens session breakdown (existing functionality preserved)
- [ ] Empty state shown when no active forwarding

## Notes

Discovered during v0.0.64 manual verification. The DirectionalStatusCard (Phase 20) was added as a separate component rather than integrated into the existing status card structure.
