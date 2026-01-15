# Fix Summary: UAT-001 Role-Specific Deletion

**Issue:** Deleting one pairing role deleted both directions in bidirectional pairing.

## Root Cause

The `roleToDelete` was correctly parsed by `CommandParser` but **never passed through the Intent** from `SmsReceiver` to `MasterService`. The role data was lost in transit.

**Data flow before fix:**
```
SMS "SMSC UNPAIR TARGET"
  → CommandParser extracts role ✓
  → SmsReceiver creates Intent (role NOT passed) ✗
  → MasterService calls handleUnpair(sender) with role=null
  → Legacy behavior: delete ALL roles
```

## Solution

Pass the role through Intent extras:

1. **SmsReceiver.kt:** Add `EXTRA_UNPAIR_ROLE` to Intent
2. **MasterService.kt:** Extract role from Intent and pass to `handleUnpair()`

## Commits

| Commit | Description |
|--------|-------------|
| `5be7313` | **Root cause fix:** Pass role through Intent from SmsReceiver to MasterService |
| `79186a1` | Fix regex word boundary (turned out to be unnecessary) |
| `66348b6` | Make UNPAIR command role-specific in protocol |
| `612ab30` | Use role-specific deletion in PairedDevicesViewModel |
| `88b373b` | Remove UNPAIR confirmation to prevent cascade |

## Files Modified

- `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandPatterns.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandParser.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/commands/ParsedCommand.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/services/SmsSender.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt`

## Verification

- ✅ Build succeeds
- ✅ All 228 unit tests pass
- ✅ Manual testing confirms role-specific deletion works
- ✅ Bidirectional pairing can be reduced to unidirectional
