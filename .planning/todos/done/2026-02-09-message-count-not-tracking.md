---
created: 2026-02-09T18:30
title: "[CRITICAL] Message count not tracking - forwarded messages not counted"
area: uat
priority: critical
files:
  - app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt
  - app/src/main/java/dev/notyouraverage/smscourier/data/dao/ForwardingSessionDao.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardedMessageRepository.kt
---

## Problem

When forwarding SMS messages, the message count in sessions is not being tracked/incremented. User has reported this 3-4 times. All sessions show 0 messages despite messages being forwarded successfully.

**Related issues to investigate:**
1. When adding a previously existing device, old sessions with no messages are visible but can't be cleaned up
2. Settings cleanup shows "no active sessions" when orphan sessions exist
3. Possible session tracking mismatch - messages may be tracked in wrong session due to session lookup bugs

**Root cause possibilities:**
- Session lookup failing to find correct session
- incrementMessagesForwarded not being called
- Phone number normalization mismatch
- Session ID mismatch between storage and counter update

## Solution

1. Write integration tests to reproduce the message counting bug
2. Trace the exact flow: SmsReceiver → incrementMessagesForwarded → verify DB update
3. Check phone number normalization consistency between session creation and message tracking
4. Verify session lookup query matches active session correctly
