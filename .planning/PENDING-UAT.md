# Pending UAT Verification

> Phases that were completed but never had UAT verification performed.
> Generated 2026-04-16

## Summary

| Milestone | Phase | Has UAT? | User-Facing? |
|-----------|-------|----------|-------------- |
| v0.0.64 | 15 - Database Foundation & Migration | No | No (infra) |
| v0.0.64 | 16 - Message Storage Integration | **Yes** (16-UAT.md) | Partial |
| v0.0.64 | 17 - Device History UI | **Yes** (17-UAT.md) | Yes |
| v0.0.64 | 18 - Session History & Message Detail | **No** | Yes |
| v0.0.64 | 19 - Export Functionality | **No** | Yes |
| v0.0.64 | 20 - Bidirectional Visibility Indicators | **No** | Yes |
| v0.0.64 | 21 - History Retention Settings | **No** | Yes |
| v0.0.64 | 22 - Auto-Cleanup WorkManager | **No** | Yes |
| v0.0.64 | 23 - UAT Bug Fixes | **No** | Yes |
| v0.0.65 | 24 - Reproduce UAT Issues | N/A | No (meta) |
| v0.0.65 | 25 - Home Screen Fixes | **No** | Yes |
| v0.0.65 | 26 - Session Management Fixes | **Yes** (26-UAT.md) | Yes |
| v0.0.65 | 27 - Device History Fixes | In progress | Yes |
| v0.0.65 | 28 - UI & Notification Quick Fixes | Not started | Yes |

**Phases requiring UAT: 15, 18, 19, 20, 21, 22, 23, 25** (8 total)

---

## Phase 15: Database Foundation & Migration

**Goal:** Database schema supports message-level storage and device archiving with validated migration

> This is infrastructure-only. UAT focuses on verifying the app survives the migration.

### Steps

1. **Fresh install verification**
   - Uninstall the app completely
   - Install the current APK
   - Open the app — verify it launches without crash
   - Navigate to Device History — should show empty state
   - **Pass criteria:** App launches, no crash, empty states render

2. **Upgrade migration verification** (if you have a v0.0.63 APK)
   - Install v0.0.63 APK, pair a device, run a session
   - Install current APK over it (upgrade)
   - Open app — verify no crash, paired device still shows
   - Navigate to Device History — paired device appears
   - **Pass criteria:** Existing data preserved after upgrade, no migration crash

---

## Phase 18: Session History & Message Detail

**Goal:** Users can view session list and drill down to message-level details

### Prerequisites
- Two paired devices with at least 2 completed forwarding sessions and several forwarded messages

### Steps

1. **Access Session History**
   - From Device History, tap a paired device
   - Tap "View Sessions" in the bottom sheet
   - **Expected:** SessionHistoryScreen opens showing list of sessions for that device

2. **Session list display**
   - Verify each session card shows: start time, end time (or "Active"), message count, duration
   - Verify sessions are ordered by most recent first
   - **Expected:** All sessions for the device are listed with correct metadata

3. **Session/Contact view toggle**
   - Look for a tab or toggle between "Sessions" and "Contacts" views
   - Switch between them
   - **Expected:** Sessions view groups by session; Contacts view groups by sender phone number

4. **Active session indicator**
   - If a session is currently active, verify it shows a distinct indicator (e.g., green dot, "Active" badge)
   - **Expected:** Active sessions are visually distinguishable from completed ones

5. **Message detail drill-down**
   - Tap a session card to expand or open message detail
   - **Expected:** MessageDetailBottomSheet appears showing forwarded messages

6. **Message list contents**
   - Verify each message row shows: sender number, message content (on TARGET), timestamp
   - On SOURCE device, verify message shows metadata only (no content)
   - **Expected:** Messages display correctly per device role

7. **Expandable message rows**
   - Tap a message row to expand it
   - **Expected:** Row expands to show full message content and additional metadata

8. **Pagination / scrolling**
   - If you have many messages (20+), scroll to the bottom
   - **Expected:** More messages load as you scroll (Paging 3), no jank or duplicate items

9. **Empty session**
   - View a session that was started and stopped with 0 messages forwarded
   - **Expected:** Empty state or "No messages forwarded" text shown

10. **Back navigation**
    - Press back from Session History
    - **Expected:** Returns to Device History screen, state preserved

---

## Phase 19: Export Functionality

**Goal:** Users can export session/message history in CSV, JSON, and TXT formats

### Prerequisites
- At least one device with completed sessions and forwarded messages
- An archived device with history (for archive export test)

### Steps

1. **Export from Session History**
   - Open Session History for a device
   - Look for an export button/icon
   - Tap it
   - **Expected:** ExportFormatBottomSheet appears with CSV, JSON, TXT options

2. **Export format selection - CSV**
   - Select CSV format
   - Toggle metadata on/off if available
   - Tap export/confirm
   - **Expected:** SAF file picker opens, save the file, success feedback shown

3. **Verify CSV file contents**
   - Open the saved CSV file
   - **Expected:** Contains header row + data rows with session/message info, properly comma-separated

4. **Export format selection - JSON**
   - Repeat export, select JSON
   - Save and open the file
   - **Expected:** Valid JSON with session and message data

5. **Export format selection - TXT**
   - Repeat export, select TXT
   - Save and open the file
   - **Expected:** Human-readable text format with session/message data

6. **Single-session export**
   - In Session History, long-press a session card (or find export button on message detail)
   - **Expected:** Export option appears for just that session

7. **Device-level export**
   - From Device History bottom sheet, tap "Export History"
   - **Expected:** Exports all sessions for that device

8. **Archive device export**
   - Navigate to Removed Devices section, open an archived device
   - Tap export
   - **Expected:** Export works for archived devices too

9. **Export with no data**
   - Try exporting from a device/session with no messages
   - **Expected:** Graceful handling — either empty file with headers or informative message

10. **Metadata toggle**
    - In ExportFormatBottomSheet, toggle the metadata option
    - Export with metadata ON and OFF
    - **Expected:** With metadata includes device info, timestamps, session details; without is content-only

---

## Phase 20: Bidirectional Visibility Indicators

**Goal:** Home screen shows directional forwarding status with smart indicators

### Prerequisites
- Two paired devices
- Be able to start forwarding sessions in both directions

### Steps

1. **No active sessions - baseline**
   - With no active sessions, check the Home screen
   - **Expected:** No directional status card shown (or shows idle state)

2. **Outbound forwarding (TARGET role)**
   - Start a session where THIS device forwards TO the other
   - Check Home screen
   - **Expected:** ↑ (up arrow) indicator shown, labeled as forwarding TO device

3. **Inbound forwarding (SOURCE role)**
   - Start a session where THIS device receives FROM the other
   - Check Home screen
   - **Expected:** ↓ (down arrow) indicator shown, labeled as receiving FROM device

4. **Bidirectional forwarding**
   - Have both directions active simultaneously
   - Check Home screen
   - **Expected:** ⇅ (bidirectional) indicator shown

5. **DirectionalStatusCard display**
   - Verify the status card shows: direction arrow, device name/number, session count
   - **Expected:** Card is visually clear about what's happening

6. **Session breakdown bottom sheet**
   - Tap the DirectionalStatusCard
   - **Expected:** SessionBreakdownBottomSheet opens showing grouped sessions with stop actions

7. **Stop from breakdown sheet**
   - In the breakdown sheet, tap stop on a session
   - **Expected:** Confirmation dialog appears, session stops on confirm, indicator updates

8. **Device card directional arrows**
   - In the paired devices list on Home screen, check device cards
   - **Expected:** Each device card shows its directional arrow if it has an active session

9. **Indicator updates on session end**
   - Stop all active sessions
   - **Expected:** Indicators disappear or return to idle state, no stale arrows

---

## Phase 21: History Retention Settings

**Goal:** Users can configure history retention and manually clean old data

### Prerequisites
- Some forwarding history (sessions and messages) older than 7 days if possible

### Steps

1. **Navigate to retention setting**
   - Open Settings screen
   - Find the History Retention option
   - **Expected:** Dropdown or selector showing retention period options

2. **Retention options available**
   - Tap the retention selector
   - **Expected:** Options include 7 days, 14 days, 30 days (default), 60 days, 90 days, Forever

3. **Default retention value**
   - On a fresh install, check the default
   - **Expected:** Default is 30 days

4. **Change retention period**
   - Select a different retention period (e.g., 7 days)
   - **Expected:** Selection saves immediately, persists after leaving and returning to Settings

5. **Manual cleanup button**
   - Find the "Clean up now" button in Settings
   - **Expected:** Button is visible and enabled

6. **Execute manual cleanup**
   - Tap "Clean up now"
   - **Expected:** Cleanup runs, shows result feedback with counts (e.g., "Deleted 3 sessions, 15 messages")

7. **Cleanup result verification**
   - After cleanup, navigate to Device History / Session History
   - **Expected:** Data older than the retention period is gone; recent data remains

8. **Forever retention**
   - Set retention to "Forever"
   - **Expected:** Clean up now button behavior adjusts (hidden or shows 0 items to clean)

---

## Phase 22: Auto-Cleanup WorkManager

**Goal:** History cleanup runs automatically on schedule based on retention settings

### Steps

1. **Auto-cleanup toggle visibility**
   - Open Settings, find the auto-cleanup toggle
   - **Expected:** Toggle is visible when retention is set to a time-based period (not Forever)

2. **Toggle hidden when Forever**
   - Set retention to "Forever"
   - **Expected:** Auto-cleanup toggle is hidden (nothing to clean)

3. **Enable auto-cleanup**
   - Set retention to 30 days, enable the auto-cleanup toggle
   - **Expected:** Toggle turns on, WorkManager schedules cleanup

4. **Last cleanup timestamp**
   - After enabling, check for "Last cleaned" display
   - **Expected:** Shows timestamp of last automatic cleanup (or "Never" if first time)

5. **Disable auto-cleanup**
   - Turn the toggle off
   - **Expected:** Toggle turns off, scheduled work is cancelled

6. **Persist across app restart**
   - Enable auto-cleanup, force-close the app, reopen
   - Navigate to Settings
   - **Expected:** Auto-cleanup toggle remains in its last state

---

## Phase 23: UAT Bug Fixes

**Goal:** Critical bugs fixed — combinedClickable crash, unpair confirmation, consolidated forwarding UI

### Steps

1. **No crash on device card tap**
   - Open Home screen, tap a paired device card
   - **Expected:** Bottom sheet or navigation occurs without crash (was: combinedClickable crash)

2. **No crash on session card tap**
   - Open Session History, tap a session card
   - **Expected:** Message detail opens without crash

3. **Long-press on device card**
   - Long-press a paired device card
   - **Expected:** Works without crash (combinedClickable fix)

4. **Unpair confirmation dialog**
   - Open device bottom sheet, tap "Unpair Device"
   - **Expected:** Confirmation dialog appears (not immediate unpair)

5. **Unpair dialog with active session**
   - With an active session, try to unpair
   - **Expected:** Dialog warns about active session being ended

6. **Unpair confirm action**
   - Confirm the unpair in the dialog
   - **Expected:** Device is unpaired and archived, removed from active list

7. **Unpair cancel action**
   - Open unpair dialog, tap Cancel
   - **Expected:** Dialog dismisses, nothing happens, device remains paired

8. **Consolidated forwarding display**
   - With active forwarding, check Home screen
   - **Expected:** Active forwarding info is shown in the DirectionalStatusCard (not duplicated elsewhere)

---

## Phase 25: Home Screen Fixes

**Goal:** Fix forwarding direction labels and active badge layout

### Prerequisites
- Two paired devices, ability to start forwarding in both directions

### Steps

1. **Forwarding direction label - TARGET device**
   - On the TARGET device (the one forwarding), start a session
   - Check the Home screen label
   - **Expected:** Label correctly says "Forwarding to [device]" (not reversed)

2. **Forwarding direction label - SOURCE device**
   - On the SOURCE device (the one receiving), check the Home screen label
   - **Expected:** Label correctly says "Receiving from [device]" (not reversed)

3. **Active badge layout**
   - With active forwarding, check the badge/indicator on device cards
   - **Expected:** Badge is properly positioned, doesn't overlap text, correct color

4. **Multiple active sessions labels**
   - With multiple sessions active, verify all labels are correct
   - **Expected:** Each direction is correctly labeled

5. **Session ends - labels clear**
   - Stop all sessions
   - **Expected:** Direction labels and badges are removed, no stale state

---

## Testing Order Recommendation

Execute UAT in this order (building up from infrastructure to UI):

1. **Phase 15** — Quick migration sanity check (5 min)
2. **Phase 18** — Session History & Messages (15 min)
3. **Phase 19** — Export Functionality (20 min)
4. **Phase 20** — Bidirectional Indicators (15 min)
5. **Phase 21** — History Retention Settings (10 min)
6. **Phase 22** — Auto-Cleanup (10 min)
7. **Phase 23** — Bug Fix Verification (10 min)
8. **Phase 25** — Home Screen Fix Verification (10 min)

**Total estimated time: ~95 minutes**
