# Phase 16: Message Storage Integration - Context

**Gathered:** 2026-02-05
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement data persistence for forwarded messages during active forwarding sessions. TARGET devices store full message details (sender, content, timestamp, destination) when forwarding SMS. SOURCE devices track session statistics without storing individual message content. Session counters update in real-time. All database operations run synchronously within transactions to guarantee consistency.

</domain>

<decisions>
## Implementation Decisions

### Role-specific storage behavior
- **TARGET device**: Stores full ForwardedMessage records with sender (original SMS sender), content, timestamp, and destinationNumber (which SOURCE received it)
- **SOURCE device**: Increments session statistics only - does NOT store individual ForwardedMessage records
- **Sender field**: Always the original SMS sender phone number (+1234567890), never the SOURCE device number
- **Destination field**: TARGET stores which SOURCE device received the forwarded message (supports multiple SOURCE scenarios)

### Real-time updates & timing
- **Session message count**: Updates immediately with each forwarded message (ForwardingSession.messageCount++)
- **Session timestamp**: ForwardingSession.updatedAt only updates on START/STOP events, NOT per message
- **Device statistics**: PairedDevice.totalSessions and totalMessagesForwarded update immediately (session start and per message)
- **Threading model**: Synchronous database writes within transactions - MasterService blocks until DB operations complete

### Error handling & resilience
- **Storage failure**: If ForwardedMessage insertion fails, continue forwarding (message still goes to SOURCE), log error only
- **Statistics failure**: If messageCount or totalMessagesForwarded update fails, queue for background reconciliation (don't block session)
- **Session validation**: Trust foreign key constraints - no pre-validation before each message insert, catch ConstraintViolationException
- **Exception handling**: Catch SQLException and log gracefully - don't crash MasterService, return and continue processing

### Session lifecycle & edge cases
- **Service crash recovery**: On MasterService restart, find all ACTIVE sessions and mark them STOPPED with crash/recovery timestamp
- **Message orphaning**: CASCADE delete - when ForwardingSession deleted, all its ForwardedMessage records automatically deleted
- **Session state mismatch**: If TARGET tries to store message but session doesn't exist, drop message silently (no emergency session creation)
- **Storage limits**: No maximum message count per session - trust retention settings (Phase 21) for cleanup

### Claude's Discretion
- Exact implementation of background reconciliation queue for failed statistics updates
- SQL transaction isolation level and retry logic
- Logging format and verbosity for storage errors
- Timestamp precision (milliseconds vs seconds)

</decisions>

<specifics>
## Specific Ideas

- Synchronous writes guarantee that if MasterService crashes, all previously forwarded messages are persisted
- Background reconciliation allows stats to be "eventually consistent" without blocking critical forwarding path
- CASCADE delete simplifies cleanup - no orphaned messages to manage manually
- Separate role behavior (TARGET stores messages, SOURCE doesn't) optimizes storage on receiving device

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope

</deferred>

---

*Phase: 16-message-storage-integration*
*Context gathered: 2026-02-05*
