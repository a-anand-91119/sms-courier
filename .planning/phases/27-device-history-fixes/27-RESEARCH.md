# Phase 27: Device History Fixes - Research

**Researched:** 2026-04-10
**Domain:** Jetpack Compose UI + Room/Repository layer — Device History bug fixes
**Confidence:** HIGH (codebase-grounded, no external docs required)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Archive action placement (DEVH-02)**
- Archive button appears **alongside** Unpair in the active device bottom sheet
- Order from top: View Sessions → Export History → Archive Device → Unpair Device
- **Archive**: OutlinedButton, neutral/onSurface color, archive icon, label "Archive Device"
- **Unpair**: TextButton, error/red color (unchanged from current)
- Available for devices in **any pairing status** (PENDING, REJECTED, APPROVED)

**Archive confirmation dialog** — two variants:
- No active session:
  ```
  Archive Device

  This removes [name/number] from your active devices and preserves history.

  Use Unpair instead if you want to notify the other device.

  [Cancel]   [Archive]
  ```
- Active session present:
  ```
  Archive Device

  ⚠️ This device has an active session. Archiving will end it. They won't be notified.

  Use Unpair instead if you want to notify the other device.

  [Cancel]   [Archive]
  ```
- On confirm: end session locally (no SMS), then call `deviceRepository.archiveDevice()`.

**Unpair dialog update** — add cross-reference to Archive:
```
Unpair Device

Remove [name/number] from your paired devices? They will be notified.

To keep history without notifying, use Archive instead.

[Cancel]   [Unpair]
```
(Same cross-reference in the active-session Unpair variant.)

**Post-archive UX**
- No snackbar — device visually moves from "Active Devices" to "Removed Devices" section via reactive flow
- Bottom sheet closes on confirm

**Removed Devices section**
- Keep current behavior: **collapsed by default**. No change to expand/collapse logic.

### Claude's Discretion

- Exact icon choice for the Archive action (Material archive icon; suggestion: `Icons.Outlined.Archive` or `Icons.Filled.Inventory2`)
- Wiring of the session-history view for removed devices (DEVH-01) — how to surface sessions when tapping a removed device
- Internal state-variable naming (e.g., `deviceToArchive`, `deviceToArchiveHasActiveSession`)
- Where to place the DEVH-02 `archiveDevice()` SMS-less logic inside the ViewModel (follow `unpairDevice()` pattern minus SMS step)

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEVH-01 | Removed/archived devices show their session and message history when viewed | ArchiveManagementScreen currently omits session list entirely — must add sessions UI or reroute tap to SessionHistoryScreen. Repository already returns data correctly (verified by Phase 24 test). |
| DEVH-02 | User can explicitly archive a paired device | `PairedDeviceRepository.archiveDevice()` already exists; `DeviceHistoryViewModel` must expose a matching `archiveDevice(device)` that ends active session locally and skips SMS. Bottom sheet + dialog UI wiring required. |
</phase_requirements>

## Summary

Phase 27 is a pure bug-fix/UI-wiring phase over an already-mature codebase. No new libraries, no architectural changes, no new data models. Both requirements map to existing, verified infrastructure:

- **DEVH-02** ("explicit archive action") is a ViewModel + Compose wiring task. `PairedDeviceRepository.archiveDevice()` and `sessionRepository.endSessionForDevice()` already exist. The Phase 24 failing test (`UAT DEVH-02`) calls `viewModel.archiveDevice(device)` via a stub extension function that throws `NotImplementedError`. The fix: add a real `archiveDevice()` method on `DeviceHistoryViewModel` that mirrors `unpairDevice()` minus the `smsSender.sendUnpair(...)` step, then wire a new Archive button + confirmation dialog in `DeviceHistoryScreen.DeviceDetailBottomSheet`.

- **DEVH-01** ("archived device history shows empty") is mis-scoped at the repository level. The Phase 24 failing test (`UAT DEVH-01 - getSessionsForDevice returns sessions for archived device`) already PASSES at the repository layer — the comment in the test file explicitly states: *"This test PASSES at the repository level, confirming the bug is in the UI/ViewModel layer where archived device sessions are not being displayed."* Inspection of `ArchiveManagementScreen.kt` confirms the bug: the screen renders only device metadata (name, role, removal reason, totals) and export/delete buttons — it does **not** render a list of sessions or messages. A user tapping a removed device lands on this screen and sees "empty" history because the session list UI doesn't exist there.

**Primary recommendation:** For DEVH-01, make `RemovedDeviceDialog → "View History"` navigate to `SessionHistoryScreen` (which already supports paged session + contact tabs and works for any phone number, archived or not). This reuses battle-tested UI rather than duplicating it inside `ArchiveManagementScreen`. For DEVH-02, follow the `unpairDevice()` pattern exactly, minus SMS.

## Standard Stack

No new libraries. All work uses the existing SMS Courier stack.

### Core (already in project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose Material3 | existing | UI primitives (AlertDialog, ModalBottomSheet, OutlinedButton) | Project-wide convention |
| Navigation Compose | existing | `Screen.SessionHistory.createRoute(phone, role)` | Already wired for active devices |
| Room + Flow | existing | Reactive device/session queries | Drives automatic list reordering post-archive |
| MockK + JUnit4 + Robolectric | existing | Test doubles for ViewModel unit tests | Matches `DeviceHistoryViewModelTest` style |

### Don't add
- No new icon libraries (use `androidx.compose.material.icons.*` already in project)
- No new repositories or DAOs
- No new navigation routes

## Architecture Patterns

### Key existing contracts

| Component | Method | Notes |
|-----------|--------|-------|
| `PairedDeviceRepository` | `archiveDevice(phoneNumber, role, initiatedBy)` | Already exists; sets `isArchived = true` via DAO |
| `ForwardingSessionRepository` | `endSessionForDevice(phoneNumber, stoppedBy)` | Idempotent — safe to call when no active session |
| `DeviceHistoryViewModel` | `unpairDevice(device)` | Reference pattern for `archiveDevice()` (lines 152–174) |
| `DeviceHistoryViewModel` | `removedDevices: StateFlow` | Reactively updates from `deviceRepository.getArchivedDevices()` — UI will auto-refresh |
| `ForwardingSessionRepository.getSessionsForDevice(phone)` | Flow of sessions | **Already ignores `isArchived`** — no DAO change needed |
| `SessionHistoryScreen` + `SessionHistoryViewModel` | Full paged UI with Sessions/Contacts tabs | Works for any phone number regardless of archived status |
| `Screen.SessionHistory.createRoute(phone, role)` | Nav route | Already handles URL-encoded phone numbers |

### Pattern 1: ViewModel archive action (DEVH-02)

Mirror `unpairDevice()` exactly, dropping the SMS step:

```kotlin
// File: DeviceHistoryViewModel.kt — add alongside unpairDevice()
/**
 * Archives a device locally without notifying the remote device.
 * Ends any active session, then archives. Pairing relationship is preserved
 * on the remote device (no UNPAIR SMS sent).
 */
fun archiveDevice(device: PairedDevice) {
    viewModelScope.launch {
        // End any active session locally (no SMS)
        sessionRepository.endSessionForDevice(device.phoneNumber, "ARCHIVE")

        // Archive the device
        deviceRepository.archiveDevice(
            phoneNumber = device.phoneNumber,
            role = device.role,
            initiatedBy = "USER",
        )
    }
}
```

**Verification points:**
- `stoppedBy = "ARCHIVE"` distinguishes archive-initiated session ends from unpair-initiated ones in historical records
- `initiatedBy = "USER"` matches existing archival convention (used by `unpairDevice`)
- No `smsSender.sendUnpair(...)` call — this is the whole point of DEVH-02
- The Phase 24 test asserts `verify(exactly = 0) { smsSender.sendUnpair(any(), any()) }` — our implementation must satisfy this

### Pattern 2: State-driven archive dialog (DEVH-02 UI)

Replicate the existing `deviceToUnpair` / `deviceToUnpairHasActiveSession` pattern in `DeviceHistoryScreen.kt`:

```kotlin
// Existing pattern (lines 86–87) to replicate:
var deviceToUnpair by remember { mutableStateOf<PairedDevice?>(null) }
var deviceToUnpairHasActiveSession by remember { mutableStateOf(false) }

// Add parallel state:
var deviceToArchive by remember { mutableStateOf<PairedDevice?>(null) }
var deviceToArchiveHasActiveSession by remember { mutableStateOf(false) }
```

Wire the Archive button in `DeviceDetailBottomSheet` between `OutlinedButton("Export History")` and `TextButton("Unpair Device")` (exact order per CONTEXT.md: View Sessions → Export → Archive → Unpair). Note: the current sheet order is Export → View Sessions → Unpair; the phase must also reorder View Sessions above Export to match CONTEXT.md.

### Pattern 3: Routing removed devices to SessionHistoryScreen (DEVH-01)

Current `RemovedDeviceDialog` (lines 489–515) routes "View History" to `onNavigateToArchiveManagement(device.phoneNumber, device.role.name)`, which shows an info/export/delete page with **no session list**.

**Recommended fix:** Add a "View Sessions" action in `RemovedDeviceDialog` (or repurpose "View History") that calls `onNavigateToSessionHistory(...)` — the same callback already used for active devices. `SessionHistoryScreen` queries by phone+role via `sessionRepository.getSessionsForDevicePaged(phoneNumber)` which does not filter on `isArchived`, so it will work out of the box for archived devices.

Alternative: Convert the `AlertDialog` to a bottom sheet (consistent with active devices) with actions View Sessions → Export History → Delete All Data → (navigate to ArchiveManagementScreen for removal-reason metadata). The simpler fix is adequate.

**Key insight:** `ArchiveManagementScreen` is correctly named — it's a "management" (export/delete/info) screen, not a history browser. Don't try to cram a session list into it; route to the existing `SessionHistoryScreen` instead.

### Anti-Patterns to Avoid

- **Don't modify `ForwardingSessionDao` queries** for DEVH-01 — they already return the correct data. The Phase 24 test proves this. Changing the DAO would be solving the wrong problem.
- **Don't add a new `archiveDevice()` repository method** — it already exists in `PairedDeviceRepository` (lines 77–84).
- **Don't send any SMS in the archive path** — the whole DEVH-02 semantic is "local-only, remote stays paired from their perspective."
- **Don't duplicate the session list UI inside `ArchiveManagementScreen`** — reuse `SessionHistoryScreen` via navigation.
- **Don't change `removedDevices` collapsed-by-default** — CONTEXT.md locks this.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Archive DB mutation | Custom DAO update | Existing `PairedDeviceRepository.archiveDevice()` | Already handles normalization, timestamp, initiator |
| Ending an active session without SMS | Custom session-ending code | Existing `sessionRepository.endSessionForDevice(phone, "ARCHIVE")` | Idempotent, consistent with `unpairDevice()` |
| Detecting if device has an active session | New query | Existing `activeDevices.any { it.device.phoneNumber == X && it.hasActiveSession }` pattern already used for `deviceToUnpairHasActiveSession` | Same reactive flow, no extra async work |
| Session list for archived device | New composable screen | Navigate to existing `SessionHistoryScreen` via `Screen.SessionHistory.createRoute(phone, role)` | Already implements paged sessions, contacts tab, filters, export — phone-number-based, not archive-aware |
| Reactive UI refresh after archiving | Manual snackbar + refresh | Room + Flow will auto-move the device from `activeDevices` to `removedDevices` | Already wired via `getActiveDevices()` / `getArchivedDevices()` flows |

**Key insight:** Every piece of infrastructure Phase 27 needs already exists. This phase is almost pure wiring.

## Common Pitfalls

### Pitfall 1: Phone number normalization mismatch
**What goes wrong:** Tests or runtime lookups fail because the caller uses `"1234567890"` but the DB stores `"+1234567890"`.
**Why it happens:** Both `PairedDeviceRepository` and `ForwardingSessionRepository` normalize phone numbers internally (prepend `+`, strip spaces/dashes). The `DeviceHistoryViewModel` passes `device.phoneNumber` directly — already normalized because it came from the DB.
**How to avoid:** Use `device.phoneNumber` as-is in the ViewModel. Don't re-normalize.
**Warning signs:** Test assertion uses `"+1234567890"` but production code passes `"1234567890"` — check the Phase 24 test uses `+5551234567`.

### Pitfall 2: Double-ending a non-existent session
**What goes wrong:** Calling `endSessionForDevice` on a device with no active session.
**Why it happens:** Archive must work for devices in any state (PENDING, REJECTED, APPROVED), and many have no active session.
**How to avoid:** `endSessionForDevice` is already idempotent (UPDATE … WHERE is_active = 1 — a no-op if none match). Safe to call unconditionally.

### Pitfall 3: Bottom sheet state not dismissed after archive
**What goes wrong:** Bottom sheet for active device remains visible after archive confirmation, creating a stale UI reference.
**Why it happens:** `selectedActiveDevice` state is held in the screen; archive confirmation lives in a separate AlertDialog; forgetting to null out `selectedActiveDevice` on confirm leaves the sheet open over an item that just moved to "Removed."
**How to avoid:** On archive confirm, set `deviceToArchive = null`, `deviceToArchiveHasActiveSession = false`, AND `selectedActiveDevice = null` — matches how `unpairDevice` is handled (line 296).

### Pitfall 4: Test stub extension function shadows the real method
**What goes wrong:** Adding `archiveDevice()` on the ViewModel but the test file's stub extension `private fun DeviceHistoryViewModel.archiveDevice(...)` still shadows it → test still throws `NotImplementedError`.
**Why it happens:** Kotlin member functions take precedence over extension functions with the same signature, BUT only if resolved on the same type. Inside the test file, the private extension is still in scope and can win resolution in some edge cases.
**How to avoid:** **Delete** the stub extension at `DeviceHistoryViewModelTest.kt` lines 157–161 as part of the fix. CONTEXT.md explicitly calls this out in `<specifics>`.

### Pitfall 5: Ordering the sheet actions
**What goes wrong:** CONTEXT.md mandates ordering "View Sessions → Export History → Archive Device → Unpair Device," but the current bottom sheet (lines 425–477) is Export → View Sessions → Unpair. Fixing DEVH-02 without reordering leaves the UI out of spec.
**How to avoid:** Reorder as part of the Phase 27 UI task.

### Pitfall 6: Navigating to wrong destination for removed devices
**What goes wrong:** Current `RemovedDeviceDialog` sends "View History" to `ArchiveManagementScreen`, which has no session list — creating the DEVH-01 bug.
**How to avoid:** Route "View History" (or a new "View Sessions" action) to `onNavigateToSessionHistory(device.phoneNumber, device.role.name)`. The nav callback is already wired in `DeviceHistoryScreen` signature (line 74).

## Code Examples

### Example 1: Full archiveDevice ViewModel method (DEVH-02)

```kotlin
// File: app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt
// Location: Add after unpairDevice(), before permanentlyDeleteDevice()

/**
 * Archives a device locally without sending an UNPAIR SMS to the remote device.
 * - Ends any active session for the device (local only, no SMS).
 * - Archives the device so it appears in the Removed Devices section.
 * - The remote device is NOT notified; the pairing remains valid on their side.
 *
 * Use Unpair if you want to notify the other device.
 */
fun archiveDevice(device: PairedDevice) {
    viewModelScope.launch {
        sessionRepository.endSessionForDevice(device.phoneNumber, "ARCHIVE")
        deviceRepository.archiveDevice(
            phoneNumber = device.phoneNumber,
            role = device.role,
            initiatedBy = "USER",
        )
    }
}
```

### Example 2: Archive button in DeviceDetailBottomSheet

```kotlin
// File: DeviceHistoryScreen.kt — inside DeviceDetailBottomSheet, after Export, before Unpair

OutlinedButton(
    onClick = onArchive,
    modifier = Modifier.fillMaxWidth(),
) {
    Icon(
        Icons.Outlined.Archive, // or Icons.Filled.Inventory2
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("Archive Device")
}
```

And add the parameter to `DeviceDetailBottomSheet(... onArchive: () -> Unit, ...)`.

### Example 3: Archive confirmation dialog

```kotlin
// File: DeviceHistoryScreen.kt — add alongside Unpair dialog

deviceToArchive?.let { device ->
    AlertDialog(
        onDismissRequest = {
            deviceToArchive = null
            deviceToArchiveHasActiveSession = false
        },
        title = { Text("Archive Device") },
        text = {
            if (deviceToArchiveHasActiveSession) {
                Text(
                    "⚠️ This device has an active session. Archiving will end it. " +
                        "They won't be notified.\n\n" +
                        "Use Unpair instead if you want to notify the other device.",
                )
            } else {
                Text(
                    "This removes ${device.displayName ?: device.phoneNumber} " +
                        "from your active devices and preserves history.\n\n" +
                        "Use Unpair instead if you want to notify the other device.",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.archiveDevice(device)
                    deviceToArchive = null
                    deviceToArchiveHasActiveSession = false
                    selectedActiveDevice = null
                },
            ) {
                Text("Archive")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                deviceToArchive = null
                deviceToArchiveHasActiveSession = false
            }) {
                Text("Cancel")
            }
        },
    )
}
```

### Example 4: RemovedDeviceDialog routed to SessionHistoryScreen (DEVH-01)

```kotlin
// File: DeviceHistoryScreen.kt — update RemovedDeviceDialog onViewHistory callback

selectedRemovedDevice?.let { device ->
    RemovedDeviceDialog(
        device = device,
        onDismiss = { selectedRemovedDevice = null },
        onViewHistory = {
            selectedRemovedDevice = null
            // DEVH-01: route to SessionHistoryScreen (which already works for archived devices)
            onNavigateToSessionHistory(device.phoneNumber, device.role.name)
        },
        onDelete = {
            deviceToDelete = device
            selectedRemovedDevice = null
        },
    )
}
```

No change needed to `SessionHistoryScreen` or `SessionHistoryViewModel` — they are phone-number based and do not filter by `isArchived`. The `Screen.SessionHistory.createRoute` helper already exists.

### Example 5: Updated Unpair dialog cross-reference

```kotlin
// Updated text block in the existing Unpair dialog

text = {
    if (deviceToUnpairHasActiveSession) {
        Text(
            "This device has an active forwarding session. Unpairing will end the session " +
                "and remove ${device.displayName ?: device.phoneNumber} from your paired devices. " +
                "They will be notified.\n\n" +
                "To keep history without notifying, use Archive instead.",
        )
    } else {
        Text(
            "Remove ${device.displayName ?: device.phoneNumber} from your paired devices? " +
                "They will be notified.\n\n" +
                "To keep history without notifying, use Archive instead.",
        )
    }
},
```

## State of the Art

| Old Approach (pre-Phase 27) | Current Approach (post-Phase 27) | Impact |
|-----------------------------|----------------------------------|--------|
| Only `unpairDevice()` exposed — forces SMS to remote | Both `archiveDevice()` (local-only) and `unpairDevice()` (notifies remote) | Users can retire devices without tipping off the other side |
| Tapping removed device → `ArchiveManagementScreen` (metadata only) | Tapping removed device → `SessionHistoryScreen` (full paged sessions) | Reuses existing battle-tested UI for history browsing |
| Test stub `DeviceHistoryViewModel.archiveDevice` extension that throws | Real `archiveDevice` member method | Phase 24 UAT DEVH-02 test goes from failing → passing |

## Open Questions

1. **Should `ArchiveManagementScreen` remain reachable at all?**
   - What we know: Its value (removal reason, delete-all-data) is still useful, distinct from a session browser.
   - What's unclear: Whether Phase 27 should add a secondary action in `RemovedDeviceDialog` (e.g., "Manage / Delete") that preserves access to `ArchiveManagementScreen`.
   - Recommendation: Keep a two-action dialog — "View Sessions" (new, goes to SessionHistoryScreen) and "Manage / Delete" (existing, goes to ArchiveManagementScreen). Simple addition, preserves DELETE-ALL workflow.

2. **Icon choice for "Archive Device" button**
   - What we know: Material 3 icon set is available via `androidx.compose.material.icons.*`.
   - What's unclear: Which specific icon matches best.
   - Recommendation: `Icons.Outlined.Archive` (standard archive box glyph). Easy change during review.

3. **Should archive also record something on `PairedDevice.archivalInitiatedBy`?**
   - What we know: `PairedDeviceRepository.archiveDevice()` takes `initiatedBy: String` and sets `archivalInitiatedBy` (used by `ArchiveManagementViewModel.getRemovalReason()` which distinguishes "LOCAL" / "REMOTE" / fallback).
   - What's unclear: For a user-initiated archive, should we pass `"LOCAL"` (to match unpair's semantics and get the "You initiated the removal" message) or `"USER"` (which `unpairDevice` currently passes and which falls through to "Removal reason unknown").
   - Recommendation: Planner should audit `unpairDevice`'s `"USER"` value — it may itself be a latent bug. For Phase 27, pass `"LOCAL"` so archived devices show "You initiated the removal" correctly. Keep `unpairDevice` unchanged for this phase to avoid scope creep; flag as a follow-up todo.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + MockK + Robolectric (SDK 34) + kotlinx-coroutines-test |
| Config file | `app/build.gradle.kts` (existing) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModelTest"` |
| Full suite command | `./gradlew test` |
| UAT-only filter | Category marker `@Category(UATTest::class)` on specific tests |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DEVH-02 | `archiveDevice()` on ViewModel archives locally without UNPAIR SMS | unit | `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest.UAT DEVH-02*"` | ✅ (Phase 24 — currently fails via stub) |
| DEVH-02 | `archiveDevice()` ends active session before archiving | unit | Add to `DeviceHistoryViewModelTest` | ❌ Wave 0 — add test |
| DEVH-02 | `archiveDevice()` works for PENDING/REJECTED devices (no SMS in any case) | unit | Add to `DeviceHistoryViewModelTest` | ❌ Wave 0 — add test |
| DEVH-01 | Repository returns sessions for archived devices | unit | `./gradlew :app:testDebugUnitTest --tests "*ForwardingSessionRepositoryTest.UAT DEVH-01*"` | ✅ (Phase 24 — **already passes**; serves as regression guard) |
| DEVH-01 | Navigation: tapping a removed device routes to `SessionHistoryScreen` | manual UAT | Launch debug APK, archive a device, tap in Removed section, verify session list visible | manual only |
| DEVH-02 | End-to-end: archive button visible → confirm → device moves to Removed section | manual UAT | Launch debug APK, open Device History, open active device sheet, tap Archive | manual only |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*" --tests "*ForwardingSessionRepositoryTest*"` (≈ 10s)
- **Per wave merge:** `./gradlew testDebugUnitTest` (full JVM suite — covers all ViewModels + repositories + handlers)
- **Phase gate:** `./gradlew test spotlessCheck` green before `/gsd:verify-work`, plus manual UAT walkthrough on a physical device to confirm DEVH-01 navigation and DEVH-02 bottom-sheet ordering.

### Wave 0 Gaps

- [ ] **Delete test stub** at `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt` lines 157–161 (`private fun DeviceHistoryViewModel.archiveDevice(device: PairedDevice)`) — this is required BEFORE the production method is added, otherwise the test will still resolve to the stub.
- [ ] **Add new tests** to `DeviceHistoryViewModelTest.kt`:
  - `archiveDevice ends active session before archiving` — verify `coVerify { sessionRepository.endSessionForDevice(phone, "ARCHIVE") }` order relative to `archiveDevice()`
  - `archiveDevice does not send any SMS regardless of pairing status` — parameterize over APPROVED / PENDING_RECEIVED / REJECTED
  - `archiveDevice calls repository archiveDevice with LOCAL initiator` (if Open Question 3 is resolved to `"LOCAL"`)
- [ ] **No new test files** required — existing `DeviceHistoryViewModelTest.kt` and `ForwardingSessionRepositoryTest.kt` already provide the harness.
- [ ] **No framework install** — JUnit4/MockK/Robolectric already on classpath.

## Sources

### Primary (HIGH confidence) — codebase inspection

- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt` — existing `unpairDevice()` reference pattern, `hasActiveSession()` helper, state flows
- `app/src/main/java/dev/notyouraverage/smscourier/repository/PairedDeviceRepository.kt` — `archiveDevice()` method signature (lines 77–84)
- `app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt` — `endSessionForDevice()`, `getSessionsForDevicePaged()`
- `app/src/main/java/dev/notyouraverage/smscourier/data/dao/ForwardingSessionDao.kt` — confirms DAO queries are NOT filtered by `isArchived`
- `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/DeviceHistoryScreen.kt` — `DeviceDetailBottomSheet`, `RemovedDeviceDialog`, unpair-dialog state pattern
- `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/ArchiveManagementScreen.kt` — confirms no session list UI exists here (the DEVH-01 root cause)
- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/SessionHistoryViewModel.kt` — confirms phone-number-based paged queries, no archive filter
- `app/src/main/java/dev/notyouraverage/smscourier/navigation/Screen.kt` — `SessionHistory.createRoute()` already exists
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt` — Phase 24 failing UAT DEVH-02 test + stub extension to delete
- `app/src/test/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepositoryTest.kt` — Phase 24 UAT DEVH-01 test (already passes; see comment lines 92–94)
- `.planning/phases/27-device-history-fixes/27-CONTEXT.md` — user decisions

### Secondary
- `CLAUDE.md` — build/test/format commands, git conventions
- `.planning/STATE.md` — phase history, established patterns (Phase 26 role-based sections, bidirectional handling)
- `.planning/REQUIREMENTS.md` — DEVH-01/02 definitions

### Tertiary
- None — phase is entirely self-contained within the codebase. No external library research required.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries, all primitives verified present
- Architecture: HIGH — patterns directly mirror existing `unpairDevice()` flow verified in source
- Pitfalls: HIGH — each pitfall traced to a specific line in existing code
- DEVH-01 root cause: HIGH — confirmed by reading `ArchiveManagementScreen.kt` (no session list UI) and the comment in `ForwardingSessionRepositoryTest.kt` lines 92–94
- DEVH-02 implementation path: HIGH — reference implementation (`unpairDevice`) and failing test both inspected

**Research date:** 2026-04-10
**Valid until:** 2026-05-10 (30 days — stable codebase, no external API surface)
