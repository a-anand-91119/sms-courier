# UAT Issues: Phase 5 Plan 01

**Tested:** 2026-01-15
**Source:** .planning/phases/05-bidirectional-pairing/05-01-SUMMARY.md
**Tester:** User via /gsd:verify-work

## Open Issues

### UAT-001: Deleting one role deletes both directions

**Discovered:** 2026-01-15
**Phase/Plan:** 05-01
**Severity:** Major
**Feature:** Role-specific deletion in bidirectional pairing
**Description:** When deleting a phone number from one section (e.g., "I forward to" / SOURCE role), it also deletes the same phone number from the other section ("Forwarded to me" / TARGET role). This defeats the purpose of bidirectional pairing as you cannot maintain one direction while removing the other.

**Expected:** Deleting a device from "I forward to" should only remove the SOURCE role, leaving the TARGET role intact. Each role should be independently deletable.

**Actual:** Deleting from one section removes the entry from both sections.

**Root Cause (Suspected):** The unpair acknowledgment logic may be causing this. When device A sends UNPAIR to device B, device B acknowledges with its own UNPAIR command. Device A might be treating this acknowledgment as a separate unpair request and deleting the other role.

**Repro:**
1. Create bidirectional pairing (same phone number exists as both SOURCE and TARGET)
2. Verify phone number appears in both "Forwarded to me" and "I forward to" sections
3. Delete the device from one section (e.g., "I forward to")
4. Observe that it also disappears from the other section ("Forwarded to me")

**Impact:** Users cannot maintain one-way pairing after establishing bidirectional pairing. If they want to stop forwarding in one direction, they lose both directions.

## Resolved Issues

[None yet]

---

*Phase: 05-bidirectional-pairing*
*Plan: 01*
*Tested: 2026-01-15*
