---
status: resolved
trigger: "message-count-not-tracking: Forwarded messages not being counted - all sessions show 0 messages"
created: 2026-02-09T00:00:00Z
updated: 2026-02-09T00:00:00Z
---

## Current Focus

hypothesis: CONFIRMED - ROOT CAUSE IDENTIFIED
test: N/A - Code analysis complete
expecting: N/A
next_action: Apply fix - make SmsReceiver use commandHandler.handleIncomingSms instead of direct DAO calls

## Symptoms

expected: When messages are forwarded from TARGET to SOURCE, the message count in forwarding sessions should increment. Device stats should show total messages forwarded.
actual: All sessions show 0 messages despite messages being forwarded successfully. Device history shows 0 messages. Session history shows 0 messages. Export shows 0 messages.
errors: No error messages - the forwarding itself works (messages arrive), just the counter doesn't increment.
reproduction:
1. Pair two devices (SOURCE and TARGET)
2. Start a forwarding session
3. Send SMS to TARGET device
4. TARGET forwards message to SOURCE (message arrives)
5. Check session history - shows 0 messages
6. Check device history stats - shows 0 messages
timeline: Issue was reported multiple times during UAT. A previous fix attempt added phone number normalization (commit d4a4b97) but issue persists.

## Eliminated

## Evidence

- timestamp: 2026-02-09T00:05:00Z
  checked: ForwardingSession entity and DAO
  found: TWO counter fields exist - messagesForwarded (legacy) and messageCount (new). TWO DAO methods exist - incrementMessagesForwarded and incrementMessageCount. UI reads messageCount field.
  implication: Need to verify which counter is being updated when messages are forwarded

- timestamp: 2026-02-09T00:06:00Z
  checked: SmsCommandHandler.handleIncomingSms() code flow
  found: Two increment paths - (1) storeMessageWithCounters calls sessionDao.incrementMessageCount inside transaction, (2) recordForwardedMessage calls incrementMessagesForwarded. Both SHOULD update counts.
  implication: If storeMessageWithCounters fails (catches exception), messageCount won't increment but forwarding continues

- timestamp: 2026-02-09T00:10:00Z
  checked: UI code - DeviceHistoryScreen.kt and SessionHistoryComponents.kt
  found:
    - Device stats show device.totalMessagesForwarded (PairedDevice entity)
    - Session list shows session.messageCount (ForwardingSession entity)
  implication: THREE counters are involved - device.totalMessagesForwarded, session.messageCount, session.messagesForwarded

- timestamp: 2026-02-09T00:12:00Z
  checked: storeMessageWithCounters transaction
  found: Transaction increments (1) session.message_count via incrementMessageCount, (2) device.total_messages_forwarded via incrementTotalMessagesForwarded. But it calls deviceDao with normalized phone AND DeviceRole.TARGET
  implication: CRITICAL - the device stats are updated for TARGET role, but user likely views SOURCE role stats!

- timestamp: 2026-02-09T00:20:00Z
  checked: SmsReceiver.handleRegularSms() vs SmsCommandHandler.handleIncomingSms()
  found: **ROOT CAUSE** - TWO separate code paths exist!
    1. SmsReceiver.handleRegularSms() (lines 128-177) - ACTUALLY RUNS when SMS arrives
       - Only calls sessionDao.incrementMessagesForwarded() (updates messages_forwarded column)
       - Sends FORWARD_SMS intent to MasterService
       - Does NOT call storeMessageWithCounters()
    2. SmsCommandHandler.handleIncomingSms() (lines 374-409) - NEVER CALLED!
       - Contains the NEW code that calls storeMessageWithCounters()
       - Would increment message_count AND total_messages_forwarded
       - But this method has NO callers!
  implication: The UI shows session.messageCount and device.totalMessagesForwarded which are NEVER incremented. The old messages_forwarded column IS incremented but UI doesn't show it.

## Resolution

root_cause: SmsReceiver.handleRegularSms() bypasses SmsCommandHandler.handleIncomingSms() and directly calls sessionDao.incrementMessagesForwarded(). The new message counting code in handleIncomingSms() (which calls storeMessageWithCounters) is NEVER invoked. UI shows messageCount/totalMessagesForwarded which are never updated.
fix:
1. Added new PROCESS_REGULAR_SMS action to MasterService
2. SmsReceiver.handleRegularSms() now sends PROCESS_REGULAR_SMS intent instead of doing direct DB calls
3. MasterService.handleRegularSms() delegates to commandHandler.handleIncomingSms()
4. This routes through the proper counting path (storeMessageWithCounters)
verification: Unit tests pass (350 tests). New tests added for storeMessageWithCounters call and failure resilience.
files_changed:
- app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt
- app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt
- app/src/test/java/dev/notyouraverage/smscourier/receivers/SmsReceiverTest.kt
- app/src/test/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandlerTest.kt
