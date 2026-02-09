# UAT Issue: Auth Challenge Not Sent Back After AUTH_REQUEST

**Found:** 2026-02-09
**Severity:** Critical (blocking)
**Flow:** Start Forwarding Session

## Issue

When trying to start a forwarding session:
- SOURCE device sends `SMSC AUTH_REQUEST` ✓
- TARGET device does NOT send back `SMSC AUTH_CHALLENGE` ✗
- Forwarding session cannot start

## Steps to Reproduce

1. Have two paired devices (SOURCE and TARGET)
2. On SOURCE device, tap "Start Forwarding"
3. Observe SMS log - `SMSC AUTH_REQUEST` is sent
4. No `SMSC AUTH_CHALLENGE` received back from TARGET

## Expected Behavior

1. SOURCE sends `SMSC AUTH_REQUEST`
2. TARGET receives and processes AUTH_REQUEST
3. TARGET generates challenge and sends `SMSC AUTH_CHALLENGE <challenge>`
4. SOURCE receives challenge, prompts for password
5. Authentication flow continues

## Possible Causes

1. TARGET device not receiving/processing incoming SMS
2. SmsReceiver not parsing AUTH_REQUEST command correctly
3. SmsCommandHandler not handling AUTH_REQUEST
4. AUTH_CHALLENGE not being sent (SmsSender issue)
5. MasterService not running on TARGET device
6. Command pattern mismatch in CommandPatterns.kt

## Files to Investigate

- `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandPatterns.kt` (AUTH_REQUEST pattern)
- `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandParser.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt` (handleAuthRequest)
- `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/services/SmsSender.kt` (sendAuthChallenge)

## Debug Steps

1. Check if TARGET device has MasterService running
2. Add logging to SmsReceiver to confirm AUTH_REQUEST is received
3. Check if device is in lockout state
4. Verify pairing status on TARGET device
