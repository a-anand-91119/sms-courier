# Phase 24: Reproduce UAT Issues - Context

**Gathered:** 2026-02-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Write failing tests that prove each of the 7 testable UAT issues (HOME-01, SESS-01/02/03, DEVH-01/02, NOTF-01) exist before any production code is changed. HOME-02 and HIST-01 are UI/icon issues verified manually in later phases.

</domain>

<decisions>
## Implementation Decisions

### Failing test strategy
- Tests must be written so they fail now but will pass once production code is fixed — no test modifications needed in fix phases (25-28)
- A grouped test suite or tag should exist so all UAT tests can be run together for a quick red/green summary of outstanding issues

### Test level
- NOTF-01 (notification approve action): Test the handler/approval logic that the notification action triggers, not the notification dispatch itself
- DEVH-01/02 (device history bugs): Test through the repository layer, not DAO directly — closer to how the app uses the data
- Other issues: Claude picks the appropriate test level (unit vs integration) per issue

### Coverage depth
- A test suite/tag grouping all UAT tests for smoke-test-style summary of which issues remain unfixed

### Claude's Discretion
- Whether to use @Ignore or let tests actively fail (pick what fits existing infrastructure)
- Test organization: dedicated UAT test class vs tests near related code
- Test naming: whether to include UAT issue IDs in test names
- Number of tests per issue (1 focused test vs 2-3 with edge cases)
- Test documentation/comments approach
- SESS-01/02/03 structure: separate independent tests vs connected scenario
- SESS-03 SMS verification: capturing actual SMS vs verifying function call
- Test level for HOME-01, SESS-01/02/03 (unit vs integration)

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 24-reproduce-uat-issues*
*Context gathered: 2026-02-16*
