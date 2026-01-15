# UAT Issues: Phase 7 Plan 1

**Tested:** 2026-01-15
**Source:** .planning/phases/07-ui-polish/07-01-SUMMARY.md
**Tester:** User via /gsd:verify-work

## Open Issues

[None]

## Resolved Issues

### UAT-001: "Approve and Create" button text wraps to two lines

**Discovered:** 2026-01-15
**Phase/Plan:** 07-01
**Severity:** Major
**Feature:** Password creation dialog when approving a device
**Description:** The "Approve and Create" button text wraps to two lines on the password creation screen
**Expected:** Button text fits on single line
**Actual:** Text wraps, looks broken
**Repro:**
1. Receive a pairing request from another device
2. Go to approve the request
3. Observe the "Approve and Create" button in password creation dialog

**Resolution:** Changed button text from "Approve & Create" to "Approve" - context makes action clear
**Fixed in:** 07-01-FIX, commit 54113c9
**Fixed date:** 2026-01-15

### UAT-002: Revert segmented button labels to original text

**Discovered:** 2026-01-15
**Phase/Plan:** 07-01
**Severity:** Minor
**Feature:** PairedDevicesScreen segmented buttons
**Description:** User prefers original "Forward To Me (N)" and "I Forward To (N)" labels over the shortened "Incoming (N)" and "Outgoing (N)"
**Expected:** Keep original labels that were more descriptive
**Actual:** Labels were changed to shorter versions
**Repro:** Navigate to Paired Devices screen and observe tab labels

**Resolution:** Restored original labels, kept ellipsis overflow as safety measure
**Fixed in:** 07-01-FIX, commit 9577e05
**Fixed date:** 2026-01-15

---

*Phase: 07-ui-polish*
*Plan: 01*
*Tested: 2026-01-15*
