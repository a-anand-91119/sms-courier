# UAT Issues: Phase 6 Plan 2

**Tested:** 2026-01-15
**Source:** .planning/phases/06-pending-state-ux/06-02-SUMMARY.md
**Tester:** User via /gsd:verify-work

## Open Issues

### UAT-001: Live countdown timer in dropdown menu

**Discovered:** 2026-01-15
**Phase/Plan:** 06-02
**Severity:** Minor
**Feature:** Resend rate limiting feedback in dropdown menu
**Description:** When viewing the dropdown menu during cooldown, the remaining seconds are static. User must close and reopen the menu to see the updated countdown.
**Expected:** Live countdown that decrements while the menu is open (e.g., "Resend (45s)" → "Resend (44s)" → ...)
**Actual:** Static number that only updates when menu is reopened
**Notes:** Functionality works correctly; this is a UX polish enhancement.

### UAT-002: Dropdown menu always appears on left side of screen

**Discovered:** 2026-01-15
**Phase/Plan:** 06-02
**Severity:** Minor
**Feature:** Dropdown menu positioning on long-press
**Description:** When long-pressing on a device card, the dropdown menu always appears on the left side of the screen/list regardless of where the press occurred.
**Expected:** Menu appears anchored near the press location or right-aligned to the card
**Actual:** Menu always appears on left side
**Notes:** Functionality works correctly; this is a UX polish enhancement for better menu positioning.

## Resolved Issues

[None yet]

---

*Phase: 06-pending-state-ux*
*Plan: 02*
*Tested: 2026-01-15*
