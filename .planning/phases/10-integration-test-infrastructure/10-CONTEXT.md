# Phase 10: Integration Test Infrastructure - Context

**Gathered:** 2026-01-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Set up infrastructure for integration testing of multi-component flows. This includes test fixtures, mock boundaries, and CI integration. The actual flow tests (pairing, forwarding, error scenarios) are written in Phase 11 — this phase builds the foundation.

</domain>

<decisions>
## Implementation Decisions

### Mock boundaries
- **MasterService**: Real service — test actual service behavior
- **SmsSender**: Mock — capture outgoing messages for verification without sending
- **Database**: In-memory Room — fast, isolated, resets between tests
- **SecurityManager**: Real — test actual password validation and lockout logic (bcrypt)
- **SmsReceiver**: Broadcast simulation — send actual intents through Robolectric (not reflection)
- **SmsCommandHandler**: Real handler — test actual command processing end-to-end
- **Repositories**: Real — use actual repositories with in-memory Room underneath
- **Android Context**: Robolectric shadow — JVM tests with mocked Android framework

### Test fixture design
- Data creation: Claude's discretion (factory methods vs builders)
- Scenario scope: Both helpers AND pre-built scenarios (data helpers + common scenario shortcuts)
- SMS intent construction: Claude's discretion on factory vs inline
- State isolation: Reset each test — clean slate, no order dependencies

### CI integration
- Test runtime: Robolectric JVM — fast, no emulator needed
- CI stage: Separate integration stage — dedicated job with own resources
- Stage ordering: Run in parallel with unit tests for faster feedback
- Blocking: Integration test failure blocks deployment
- Test reports: Published as CI artifacts (HTML/XML)
- Coverage tracking: No — just pass/fail results
- Test selection: Claude's discretion (source set vs annotation filtering)
- Performance budget: Claude's discretion

### Coverage scope
- Flow selection: Both pairing AND forwarding flows
- Error scenarios: Infrastructure supports both happy paths AND error fixtures (auth failures, lockouts, invalid commands)
- Session expiry: Deferred to Phase 11 — basic infrastructure now
- Validation: Include sample integration test to prove infrastructure works

### Claude's Discretion
- Coroutine dispatcher approach (TestDispatcher vs real)
- System time mocking approach
- Data factory pattern (factory methods vs builders)
- SMS intent construction approach
- Test selection mechanism (source set vs annotation)
- Performance budgets and timeouts

</decisions>

<specifics>
## Specific Ideas

- Build on Phase 9 patterns — Robolectric with Room in-memory database already working
- The sample test should validate the full stack works (broadcast → service → database → mock sender)
- Fixtures should make it easy to set up "paired and ready to forward" scenarios

</specifics>

<deferred>
## Deferred Ideas

- Session expiry/timeout test support — Phase 11
- Actual flow tests (full pairing, full forwarding) — Phase 11
- Instrumented tests on real emulator — not needed with Robolectric approach

</deferred>

---

*Phase: 10-integration-test-infrastructure*
*Context gathered: 2026-01-18*
