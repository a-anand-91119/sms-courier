# UAT Issues: Phase 5 Plan 01

**Tested:** 2026-01-15
**Source:** .planning/phases/05-bidirectional-pairing/05-01-SUMMARY.md
**Tester:** User via /gsd:verify-work

## Open Issues

[None]

## Resolved Issues

### UAT-001: Deleting one role deletes both directions ✅

**Discovered:** 2026-01-15
**Resolved:** 2026-01-15
**Phase/Plan:** 05-01
**Severity:** Major
**Feature:** Role-specific deletion in bidirectional pairing

**Description:** When deleting a phone number from one section (e.g., "I forward to" / SOURCE role), it also deletes the same phone number from the other section ("Forwarded to me" / TARGET role).

**Root Cause:** The `roleToDelete` was correctly parsed by `CommandParser` but never passed through the Intent from `SmsReceiver` to `MasterService`. The role data was lost in transit, causing `handleUnpair()` to receive `role=null` and trigger legacy behavior (delete all roles).

**Fix:**
- Added `EXTRA_UNPAIR_ROLE` Intent extra in `SmsReceiver`
- Extract role in `MasterService` and pass to `handleUnpair()`
- Made UNPAIR command protocol role-specific: `SMSC UNPAIR [SOURCE|TARGET]`

**Commits:** `5be7313`, `79186a1`, `66348b6`, `612ab30`, `88b373b`

**Verification:** Manual testing confirms role-specific deletion now works correctly.

---

*Phase: 05-bidirectional-pairing*
*Plan: 01*
*Tested: 2026-01-15*
