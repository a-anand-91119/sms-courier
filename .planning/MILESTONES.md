# Project Milestones: SMS Courier

## v0.0.7–v0.0.10 Feature Improvements (Shipped: 2026-01-15)

**Delivered:** Bidirectional pairing architecture, improved pending state UX with rate limiting and contextual menus, and UI polish for production readiness.

**Phases completed:** 5-7 (6 main plans + 3 fix plans)

**Key accomplishments:**
- Bidirectional pairing: Same phone number can be SOURCE and TARGET simultaneously (composite PK architecture)
- Rate-limited resend: Max 5 attempts with 1-minute cooldown for pairing requests
- Contextual UX: Dropdown menus on long-press with status-aware actions
- Notification deep-linking: Approve button navigates to pairing screen
- Smart UNPAIR: Only sends SMS for approved devices (saves SMS charges)
- Service toggle loading state with polling mechanism

**Stats:**
- 30+ files created/modified
- Database migrations 3→4→5
- 228 unit tests passing
- 1 day from start to ship

**Git range:** v0.0.7 to v0.0.10

---

## v0.0.60–v0.0.61 CI/CD Optimizations (Complete: 2026-01-16)

**Delivered:** Optimized CI/CD pipeline with upload-only lanes, proper changelog generation, and GitLab release automation.

**Phases completed:** 8 (3 plans)

**Key accomplishments:**
- Upload-only Fastlane lanes that skip unnecessary rebuilds
- Integrated fastlane-plugin-changelog for proper changelog generation
- GitLab release automation with APK/AAB artifact attachment

**Git range:** v0.0.60 to v0.0.61

---

## v0.0.62 Testing (Shipped: 2026-01-18)

**Delivered:** Comprehensive automated testing infrastructure with SmsReceiver unit tests, integration test framework, and end-to-end flow tests for pairing, forwarding, and security scenarios.

**Phases completed:** 9-11 (8 plans)

**Key accomplishments:**
- SmsReceiver unit tests (28 tests) with Robolectric covering validation, command routing, and forwarding logic
- Integration test infrastructure: IntegrationTestBase, CapturingSmsSender, ScenarioBuilders
- Pairing flow integration tests (12 tests) covering request/approve/reject/unpair/bidirectional
- Forwarding flow integration tests (13 tests) covering auth/start/forward/stop
- Security integration tests (15 tests) covering lockout, failed auth, idempotency, error handling
- Bug discovered and fixed during testing: NPE in SmsReceiver.kt

**Stats:**
- 37 files created/modified
- ~2,400 lines of test code added
- 75 new tests (28 unit + 47 integration)
- 301 total tests passing
- 1 day from start to ship

**Git range:** v0.0.62

---

_See [milestones/](milestones/) for archived milestone details._
