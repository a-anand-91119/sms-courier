# Phase 27: Device History Fixes - Context

**Gathered:** 2026-04-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix two bugs in Device History:
1. **DEVH-01**: Session history for archived/removed devices returns empty — a query bug in `ForwardingSessionRepository`
2. **DEVH-02**: No explicit "Archive" action exists on paired devices — `DeviceHistoryViewModel.archiveDevice()` is missing

Archive = local-only (no SMS to remote device, history preserved). Distinct from Unpair (notifies remote, also archives).

</domain>

<decisions>
## Implementation Decisions

### Archive action placement (DEVH-02)
- Archive button appears **alongside** Unpair in the active device bottom sheet
- Order from top: View Sessions → Export History → Archive Device → Unpair Device
- **Archive**: OutlinedButton, neutral/onSurface color, archive icon (🗂️), label "Archive Device"
- **Unpair**: TextButton, error/red color (unchanged from current)
- Available for devices in **any pairing status** (PENDING, REJECTED, APPROVED)

### Archive confirmation dialog
- Two variants based on whether device has an active session:

**No active session:**
```
Archive Device

This removes [name/number] from your active devices and preserves history.

Use Unpair instead if you want to notify the other device.

[Cancel]   [Archive]
```

**Active session present:**
```
Archive Device

⚠️ This device has an active session. Archiving will end it. They won't be notified.

Use Unpair instead if you want to notify the other device.

[Cancel]   [Archive]
```

On confirm: end session locally (no SMS), then call `deviceRepository.archiveDevice()`.

### Unpair dialog update
- Add cross-reference to Archive option in the Unpair dialog:
```
Unpair Device

Remove [name/number] from your paired devices? They will be notified.

To keep history without notifying, use Archive instead.

[Cancel]   [Unpair]
```
(Same for the active-session variant of Unpair dialog)

### Post-archive UX
- No snackbar needed — device visually moves from "Active Devices" to "Removed Devices" section
- Bottom sheet closes on confirm
- The reactive state flow handles the UI update automatically

### Removed Devices section default state
- Keep current behavior: **collapsed by default**
- No change to expand/collapse logic

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DeviceHistoryViewModel.unpairDevice()`: ends session → sends UNPAIR SMS → archives. New `archiveDevice()` follows same pattern minus the SMS step
- `PairedDeviceRepository.archiveDevice(phoneNumber, role, initiatedBy)`: already exists and works
- `ForwardingSessionRepository.endSessionForDevice()`: already exists, used by unpair — reuse for archive with active session
- `DeviceDetailBottomSheet`: composable in `DeviceHistoryScreen.kt` — add Archive button between Export and Unpair
- `deviceToUnpair` / `deviceToUnpairHasActiveSession` state pattern: already exists in screen — replicate for `deviceToArchive` / `deviceToArchiveHasActiveSession`
- `activeDevices.any { it.device == selectedDevice && it.hasActiveSession }`: pattern for checking active session on the selected device

### Established Patterns
- `viewModelScope.launch { ... }` for async operations in ViewModel
- `initiatedBy = "USER"` string convention for archival
- `deviceToUnpair` / `deviceToDelete` nullable state vars for dialog management — follow same pattern
- Role-keyed list items: `key = { "${it.device.phoneNumber}_${it.device.role}" }` already in place

### Integration Points (DEVH-01)
- `ForwardingSessionRepository` or its DAO needs to return sessions for archived devices
- Current query likely filters on `isArchived = false` for the device join — needs to include archived devices
- The `ArchiveManagementViewModel` calls into the session/export data for archived devices — this is the consumer that's currently seeing empty data
- Fix is in the DAO/repository query, not the UI

</code_context>

<specifics>
## Specific Ideas

- Archive dialog and Unpair dialog should cross-reference each other so users pick the right action
- Active-session warning in the archive dialog uses ⚠️ prefix on the warning line (same tone as Phase 26 stop-session warning patterns)
- The test stub in `DeviceHistoryViewModelTest.kt` (lines 157-161) must be replaced with the real `archiveDevice()` method — no extension function needed after the fix

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 27-device-history-fixes*
*Context gathered: 2026-04-10*
