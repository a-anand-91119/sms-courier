# Phase 11: End-to-End Flow Tests - Context

**Gathered:** 2026-01-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Create integration tests for complete pairing and forwarding flows using the infrastructure from Phase 10 (IntegrationTestBase, CapturingSmsSender, ScenarioBuilders). Tests verify multi-step flows work end-to-end, not just individual components.

</domain>

<decisions>
## Implementation Decisions

### Test organization
- One class per flow (PairingFlowTest, ForwardingFlowTest, SessionFlowTest, etc.)
- Each test method is isolated — sets up own state, runs independently
- Use @Nested inner classes to group related tests within a class (happy path, errors, edge cases)
- Test names follow Given-When-Then convention: `givenPairedDevices_whenStartForward_thenSessionCreated`
- Tests live in existing integration test directory following Phase 10 pattern

### Test coverage scope
- Comprehensive pairing scenarios: request, approve, reject, unpair, already paired, pending states
- Comprehensive forwarding scenarios: auth failure, lockout, expired session, concurrent sessions
- Explicit bidirectional pairing tests — important since v0.1 added this capability
- Multi-device scenarios with 3+ devices (one source forwarding from multiple targets)

### Assertion granularity
- Full verification: database state, SMS commands sent, and internal service state
- Exact SMS content assertion — verify full text matches expected SMSC command format
- Full ForwardingSession state verification: session created, active flag, message counts, expiry time
- Checkpoint assertions at intermediate states during multi-step flows

### Edge case priority
- High priority: authentication lockout flow (failed attempts -> lockout -> lockout expiry)
- Test all invalid state transitions (UNPAIR when not paired, START_FORWARD without pairing, etc.)
- Security critical: explicitly test commands from unknown (unpaired) devices are rejected
- Full fuzzing of malformed SMS commands (partial commands, wrong formats, injection attempts)
- Idempotency tests: verify duplicate commands are handled gracefully
- High priority: race condition tests with rapid successive commands
- Role-specific UNPAIR command tests: both UNPAIR SOURCE and UNPAIR TARGET variants

### Claude's Discretion
- ScenarioBuilder usage balance (minimal setup vs builder-heavy)
- Shared test utilities vs per-class helpers (based on code duplication)
- Test file location within existing structure
- Test class size limits (split when readability suffers)
- Time simulation for session expiry (assess feasibility with current infrastructure)

</decisions>

<specifics>
## Specific Ideas

- Tests should prove the infrastructure from Phase 10 works for real flows, not just validation
- Given-When-Then naming makes test failures self-documenting
- Nested classes keep large test files navigable

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 11-e2e-flow-tests*
*Context gathered: 2026-01-18*
